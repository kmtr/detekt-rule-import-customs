package com.github.kmtr.detektimportcustoms

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

        val config = TestConfig(Pair("patterns", arrayListOf(
            "^com.example.app::^java\\.text.*"
        )))

        val findings = ImportCustomsRule(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }
}
