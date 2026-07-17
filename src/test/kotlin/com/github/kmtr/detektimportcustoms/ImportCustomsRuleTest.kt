package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ImportCustomsRuleTest {

    @Test
    fun `reports a prohibited import`() {
        val findings = rule(
            restriction(
                from = "^com\\.example\\.app$",
                disallow = listOf("^java\\.text\\..*$"),
            ),
        ).lint(
            """
            package com.example.app

            import java.text.NumberFormat

            class Example(val formatter: NumberFormat)
            """.trimIndent(),
        )

        findings shouldHaveSize 1
    }

    @Test
    fun `does not report an allowed import`() {
        val findings = rule(
            restriction(
                from = "^com\\.example\\.app$",
                disallow = listOf("^java\\.text\\..*$"),
                allow = listOf("^java\\.text\\.NumberFormat$"),
            ),
        ).lint(
            """
            package com.example.app

            import java.text.NumberFormat

            class Example(val formatter: NumberFormat)
            """.trimIndent(),
        )

        findings.shouldBeEmpty()
    }

    @Test
    fun `matches an aliased import by its original path`() {
        val findings = rule(
            restriction(
                from = "^com\\.example\\.app$",
                disallow = listOf("^java\\.text\\.NumberFormat$"),
            ),
        ).lint(
            """
            package com.example.app

            import java.text.NumberFormat as Formatter

            class Example(val formatter: Formatter)
            """.trimIndent(),
        )

        findings shouldHaveSize 1
    }

    @Test
    fun `includes the configured reason in the finding`() {
        val findings = rule(
            restriction(
                from = "^com\\.example\\.app$",
                disallow = listOf("^java\\.text\\..*$"),
                reason = "Use the project formatter abstraction.",
            ),
        ).lint(
            """
            package com.example.app

            import java.text.NumberFormat
            """.trimIndent(),
        )

        findings.single().message shouldBe
            "`java.text.NumberFormat` is prohibited in `com.example.app`: Use the project formatter abstraction."
    }

    @Test
    fun `rejects the legacy patterns option`() {
        val exception = shouldThrow<Config.InvalidConfigurationError> {
            ImportCustomsRule(
                TestConfig("patterns" to listOf("^com.example::^java.text.*")),
            )
        }

        exception.cause?.message shouldBe
            "The 'patterns' option has been replaced by structured 'restrictions'."
    }

    @Test
    fun `rejects malformed restrictions`() {
        val exception = shouldThrow<Config.InvalidConfigurationError> {
            rule(
                mapOf(
                    "from" to "^com\\.example$",
                    "disallow" to "^java\\.text$",
                ),
            )
        }

        exception.cause?.message shouldBe
            "restrictions[0].disallow must be a list of non-blank strings."
    }

    @Test
    fun `rejects invalid regular expressions`() {
        val exception = shouldThrow<Config.InvalidConfigurationError> {
            rule(
                restriction(
                    from = "[",
                    disallow = listOf("^java\\.text$"),
                ),
            )
        }

        exception.cause?.message shouldBe
            "restrictions[0].from is not a valid regular expression: Unclosed character class."
    }

    private fun rule(vararg restrictions: Map<String, Any>): ImportCustomsRule =
        ImportCustomsRule(TestConfig("restrictions" to restrictions.toList()))

    private fun restriction(
        from: String,
        disallow: List<String>,
        allow: List<String> = emptyList(),
        reason: String? = null,
    ): Map<String, Any> = buildMap {
        put("from", from)
        put("disallow", disallow)
        if (allow.isNotEmpty()) {
            put("allow", allow)
        }
        if (reason != null) {
            put("reason", reason)
        }
    }
}
