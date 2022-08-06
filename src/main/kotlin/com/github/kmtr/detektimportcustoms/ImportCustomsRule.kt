package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtImportList

class ImportCustomsRule(config: Config) : Rule(config) {

    private val configBasePackage: String by config("")
    private val configProhibitedPackages: List<String> by config(arrayListOf())

    override val issue = Issue(
        javaClass.simpleName,
        Severity.CodeSmell,
        "This rule reports...",
        Debt.FIVE_MINS,
    )

    override fun visitImportList(importList: KtImportList) {
        super.visitImportList(importList)
        val currentPackage = importList.containingKtFile.packageFqName
        if (currentPackage.toString().startsWith(configBasePackage)) {
            val bans = importList.imports.filter {
                configProhibitedPackages.contains(it.text)
            }
            bans.let {
                report(
                    CodeSmell(
                        issue, Entity.atPackageOrFirstDecl(importList.containingKtFile),
                        "%s is prohibited in this package".format(it)
                    )
                )
            }
        }
    }
}
