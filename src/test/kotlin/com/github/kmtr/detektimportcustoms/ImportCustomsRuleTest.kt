package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class ImportCustomsRuleTest {

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

        val findings = ImportCustomsRule(config).lint(code)
        findings shouldHaveSize 1
    }
}
