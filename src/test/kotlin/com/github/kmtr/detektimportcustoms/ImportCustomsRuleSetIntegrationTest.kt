package com.github.kmtr.detektimportcustoms

import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.core.config.YamlConfig
import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.util.ServiceLoader

internal class ImportCustomsRuleSetIntegrationTest {

    @Test
    fun `loads the provider and applies an actual yaml configuration`() {
        val provider = loadProvider()
        val config = YamlConfig.load(
            StringReader(
                """
                ImportCustoms:
                  DetectProhibitedImports:
                    active: true
                    restrictions:
                      - from: '^com\.example\.app(?:\..*)?'
                        disallow:
                          - '^java\.text\..*'
                          - '^java\.time\..*'
                        reason: 'Use the application abstractions.'
                """.trimIndent(),
            ),
        )

        val ruleSet = provider.instance(config.subConfig(provider.ruleSetId))
        val rule = ruleSet.rules.single() as ImportCustomsRule
        val findings = rule.lint(
            """
            package com.example.app.feature

            import java.time.Clock

            class Example(val formatter: java.text.NumberFormat, val clock: Clock)
            """.trimIndent(),
        )

        ruleSet.id shouldBe "ImportCustoms"
        findings shouldHaveSize 2
        findings.map { it.entity.location.source.line } shouldBe listOf(3, 5)
    }

    @Test
    fun `ships a default config with the provider and rule ids`() {
        val provider = loadProvider()
        val configStream = checkNotNull(javaClass.getResourceAsStream("/config/config.yml"))
        val config = configStream.reader().use(YamlConfig::load)
        val ruleConfig = config
            .subConfig(provider.ruleSetId)
            .subConfig("DetectProhibitedImports")

        ruleConfig.valueOrNull<List<*>>("restrictions") shouldNotBe null
    }

    private fun loadProvider(): ImportCustomsRuleSetProvider {
        val providers = ServiceLoader.load(RuleSetProvider::class.java)
            .filterIsInstance<ImportCustomsRuleSetProvider>()
            .toList()
        providers shouldHaveSize 1
        return providers.single()
    }
}
