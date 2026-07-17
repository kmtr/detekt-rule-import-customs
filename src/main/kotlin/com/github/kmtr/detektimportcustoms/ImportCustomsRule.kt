package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import java.util.regex.PatternSyntaxException

internal data class ImportRestriction(
    val sourcePackagePattern: Regex,
    val forbiddenReferencePatterns: List<Regex>,
    val allowedReferencePatterns: List<Regex>,
    val reason: String?,
) {
    fun appliesTo(sourcePackage: String): Boolean = sourcePackagePattern.matches(sourcePackage)

    fun prohibits(reference: String): Boolean =
        forbiddenReferencePatterns.any { it.matches(reference) } &&
            allowedReferencePatterns.none { it.matches(reference) }
}

private data class ProhibitedImport(
    val directive: KtImportDirective,
    val path: String,
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

    override fun visitImportList(importList: KtImportList) {
        super.visitImportList(importList)

        val currentPackage = importList.containingKtFile.packageFqName.asString()
        parsedRestrictions
            .filter { it.appliesTo(currentPackage) }
            .flatMap { restriction ->
                importList.imports.mapNotNull { directive ->
                    val importPath = directive.importPath?.pathStr ?: return@mapNotNull null
                    if (restriction.prohibits(importPath)) {
                        ProhibitedImport(directive, importPath, restriction.reason)
                    } else {
                        null
                    }
                }
            }.forEach { prohibitedImport ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(importList.containingKtFile),
                        buildMessage(
                            prohibitedImport.path,
                            currentPackage,
                            prohibitedImport.reason,
                        ),
                    ),
                )
            }
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
