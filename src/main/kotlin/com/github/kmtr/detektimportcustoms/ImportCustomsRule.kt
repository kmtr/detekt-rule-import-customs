package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import java.util.regex.PatternSyntaxException

internal data class ImportRestriction(
    val sourcePackagePattern: Regex,
    val forbiddenReferencePatterns: List<Regex>,
    val allowedReferencePatterns: List<Regex>,
    val reason: String?,
) {
    fun appliesTo(sourcePackage: String): Boolean = sourcePackagePattern.matches(sourcePackage)

    fun prohibitedCandidate(candidates: List<String>): String? {
        if (candidates.any { candidate -> allowedReferencePatterns.any { it.matches(candidate) } }) {
            return null
        }
        return candidates.firstOrNull { candidate ->
            forbiddenReferencePatterns.any { it.matches(candidate) }
        }
    }
}

private data class ProhibitedReference(
    val element: KtElement,
    val reference: String,
    val reason: String?,
)

class ImportCustomsRule(config: Config) : Rule(config) {
    override val issue = Issue(
        "DetectProhibitedImports",
        Severity.CodeSmell,
        "This rule reports the violations of prohibited imports",
        Debt.FIVE_MINS,
    )

    private val restrictions: List<*> by config(emptyList<Any>())
    private val patterns: List<String> by config(emptyList())
    private val parsedRestrictions = parseRestrictions(restrictions, patterns)
    private var currentPackage = ""
    private var applicableRestrictions = emptyList<ImportRestriction>()
    private val reportedQualifiedReferenceOffsets = mutableSetOf<Int>()

    override fun visitKtFile(file: KtFile) {
        currentPackage = file.packageFqName.asString()
        applicableRestrictions = parsedRestrictions.filter { it.appliesTo(currentPackage) }
        reportedQualifiedReferenceOffsets.clear()
        super.visitKtFile(file)
    }

    override fun visitImportList(importList: KtImportList) {
        super.visitImportList(importList)

        importList.imports
            .mapNotNull { directive ->
                val importPath = directive.importPath?.pathStr ?: return@mapNotNull null
                findProhibitedReference(directive, listOf(importPath))
            }.forEach(::reportProhibitedReference)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (expression.hasAncestor<KtImportDirective>() || expression.hasAncestor<KtPackageDirective>()) {
            super.visitDotQualifiedExpression(expression)
            return
        }

        val segments = expression.qualifiedNameSegments()
        if (segments == null) {
            super.visitDotQualifiedExpression(expression)
            return
        }
        reportQualifiedReference(expression, segments)
        super.visitDotQualifiedExpression(expression)
    }

    override fun visitUserType(type: KtUserType) {
        super.visitUserType(type)
        if (type.parent is KtUserType) {
            return
        }

        val segments = type.qualifiedNameSegments()
        if (segments.size > 1) {
            reportQualifiedReference(type, segments)
        }
    }

    private fun reportQualifiedReference(element: KtElement, segments: List<String>) {
        val candidates = segments.indices.reversed().map { lastIndex ->
            segments.subList(0, lastIndex + 1).joinToString(".")
        }
        val prohibitedReference = findProhibitedReference(element, candidates) ?: return
        if (reportedQualifiedReferenceOffsets.add(element.textOffset)) {
            reportProhibitedReference(prohibitedReference)
        }
    }

    private fun findProhibitedReference(
        element: KtElement,
        candidates: List<String>,
    ): ProhibitedReference? {
        val matches = applicableRestrictions.mapNotNull { restriction ->
            restriction.prohibitedCandidate(candidates)?.let { candidate ->
                candidate to restriction.reason
            }
        }
        if (matches.isEmpty()) {
            return null
        }

        val reference = candidates.first { candidate -> matches.any { it.first == candidate } }
        val reasons = matches.mapNotNull { it.second }.distinct()
        return ProhibitedReference(
            element = element,
            reference = reference,
            reason = reasons.takeIf { it.isNotEmpty() }?.joinToString("; "),
        )
    }

    private fun reportProhibitedReference(prohibitedReference: ProhibitedReference) {
        report(
            CodeSmell(
                issue,
                Entity.from(prohibitedReference.element),
                buildMessage(
                    prohibitedReference.reference,
                    currentPackage,
                    prohibitedReference.reason,
                ),
            ),
        )
    }

    private fun buildMessage(reference: String, sourcePackage: String, reason: String?): String = buildString {
        append("`")
        append(reference)
        append("` is prohibited in `")
        append(sourcePackage)
        append("`")
        if (reason != null) {
            append(": ")
            append(reason)
        }
    }
}

private fun KtExpression.qualifiedNameSegments(): List<String>? = when (this) {
    is KtNameReferenceExpression -> listOf(getReferencedName())
    is KtDotQualifiedExpression -> {
        val receiverSegments = receiverExpression.qualifiedNameSegments() ?: return null
        val selectorSegments = selectorExpression?.qualifiedNameSegments() ?: return null
        receiverSegments + selectorSegments
    }
    is KtCallExpression -> calleeExpression?.qualifiedNameSegments()
    else -> null
}

private fun KtUserType.qualifiedNameSegments(): List<String> = buildList {
    var currentType: KtUserType? = this@qualifiedNameSegments
    while (currentType != null) {
        currentType.getReferencedName()?.let(::add)
        currentType = currentType.qualifier
    }
    reverse()
}

private inline fun <reified T : KtElement> KtElement.hasAncestor(): Boolean {
    var current = parent
    while (current != null) {
        if (current is T) {
            return true
        }
        current = current.parent
    }
    return false
}

private fun parseRestrictions(
    rawRestrictions: List<*>,
    legacyPatterns: List<String>,
): List<ImportRestriction> {
    if (legacyPatterns.isNotEmpty()) {
        invalidConfiguration(
            "The 'patterns' option has been replaced by structured 'restrictions'.",
        )
    }

    return rawRestrictions.mapIndexed { index, rawRestriction ->
        val path = "restrictions[$index]"
        val values = rawRestriction as? Map<*, *>
            ?: invalidConfiguration("$path must be a map.")
        values.validateKeys(path)

        val source = values.requiredString("from", path)
        val forbidden = values.requiredStringList("disallow", path)
        if (forbidden.isEmpty()) {
            invalidConfiguration("$path.disallow must contain at least one pattern.")
        }

        ImportRestriction(
            sourcePackagePattern = source.toValidatedRegex("$path.from"),
            forbiddenReferencePatterns = forbidden.mapIndexed { patternIndex, pattern ->
                pattern.toValidatedRegex("$path.disallow[$patternIndex]")
            },
            allowedReferencePatterns = values.optionalStringList("allow", path)
                .mapIndexed { patternIndex, pattern ->
                    pattern.toValidatedRegex("$path.allow[$patternIndex]")
                },
            reason = values.optionalString("reason", path),
        )
    }
}

private fun Map<*, *>.validateKeys(path: String) {
    val unknownKeys = keys.filterNot { it in RESTRICTION_KEYS }
    if (unknownKeys.isNotEmpty()) {
        invalidConfiguration("$path contains unknown keys: ${unknownKeys.joinToString()}.")
    }
}

private fun Map<*, *>.requiredString(key: String, path: String): String =
    optionalString(key, path)
        ?: invalidConfiguration("$path.$key must be a non-blank string.")

private fun Map<*, *>.optionalString(key: String, path: String): String? {
    val value = this[key] ?: return null
    if (value !is String || value.isBlank()) {
        invalidConfiguration("$path.$key must be a non-blank string.")
    }
    return value
}

private fun Map<*, *>.requiredStringList(key: String, path: String): List<String> {
    val value = this[key] ?: invalidConfiguration("$path.$key is required.")
    return value.toStringList("$path.$key")
}

private fun Map<*, *>.optionalStringList(key: String, path: String): List<String> {
    val value = this[key] ?: return emptyList()
    return value.toStringList("$path.$key")
}

private fun Any.toStringList(path: String): List<String> {
    val values = this as? List<*>
        ?: invalidConfiguration("$path must be a list of non-blank strings.")
    return values.mapIndexed { index, value ->
        if (value !is String || value.isBlank()) {
            invalidConfiguration("$path[$index] must be a non-blank string.")
        }
        value
    }
}

private fun String.toValidatedRegex(path: String): Regex = try {
    toRegex()
} catch (exception: PatternSyntaxException) {
    invalidConfiguration("$path is not a valid regular expression: ${exception.description}.")
}

private fun invalidConfiguration(message: String): Nothing =
    throw Config.InvalidConfigurationError(IllegalArgumentException(message))

private val RESTRICTION_KEYS = setOf("from", "disallow", "allow", "reason")
