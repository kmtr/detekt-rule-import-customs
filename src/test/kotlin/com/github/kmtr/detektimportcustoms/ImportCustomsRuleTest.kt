package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class ImportCustomsRuleTest(private val env: KotlinCoreEnvironment) {

    @Test
    fun `reports inner classes`() {
        val code = """
        package com.example.app

        import java.text.NumberFormat

        class A {
          inner class B
        }
        """

        val config = TestConfig(
            mapOf(
                "basePackage" to "com.example.app",
                "prohibitedPackages" to arrayListOf<String>(
                    "java.text.NumberFormat"
                ),
            )
        )
        val findings = ImportCustomsRule(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
        System.out.println(findings[0].message)
    }

    @Test
    fun `doesn't report inner classes`() {
        val code = """
        class A {
          class B
        }
        """
        val findings = ImportCustomsRule(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
