package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtImportList

data class ImportCustomConfiguration(
    val targetPackageNamePattern: String,
    val forbiddenImportPatterns: List<String>
)

typealias ImportCustomConfigurations = List<ImportCustomConfiguration>

class ImportCustomsRule(config: Config) : Rule(config) {
    private val patterns: List<String> by config(arrayListOf())

    override val issue = Issue(
        "DetectProhibitedImports",
        Severity.CodeSmell,
        "This rule reports the violations of prohibited imports",
        Debt.FIVE_MINS,
    )

    override fun visitImportList(importList: KtImportList) {
        super.visitImportList(importList)
        val configurations: ImportCustomConfigurations = patterns.map {
            val patterns = it.split("::")
            val basePackage = patterns[0]
            val forbiddenImportPatterns = patterns[1].split(",")
            ImportCustomConfiguration(
                targetPackageNamePattern = basePackage,
                forbiddenImportPatterns = forbiddenImportPatterns
            )
        }

        val currentPackage = importList.containingKtFile.packageFqName
        configurations
            .filter {
                currentPackage.toString().matches(it.targetPackageNamePattern.toRegex())
            }.flatMap { conf ->
                importList.imports.filter { ktImport ->
                    conf.forbiddenImportPatterns.any {
                        ktImport.importPath.toString().matches(it.toRegex())
                    }
                }
            }.map { ktImportDirective ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(importList.containingKtFile),
                        "`%s` is prohibited in `%s`".format(ktImportDirective.importPath, currentPackage)
                    )
                )
            }
    }
}
