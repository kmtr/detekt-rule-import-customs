# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Kotlin/JVM project that provides a custom Detekt rule.

- `src/main/kotlin/com/github/kmtr/detektimportcustoms/` contains the rule and its `RuleSetProvider`.
- `src/main/resources/META-INF/services/` registers the provider through Java's service-loader mechanism.
- `src/main/resources/config/config.yml` defines the default Detekt configuration.
- `src/test/kotlin/com/github/kmtr/detektimportcustoms/` contains rule tests.
- `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties` configure the build; use the checked-in Gradle wrapper rather than a system Gradle installation.

Keep production and test packages aligned. When adding a rule or provider, also update the service registration or default configuration when applicable.

## Build, Test, and Development Commands

- `./gradlew build` compiles the plugin, runs tests, and creates the JAR under `build/libs/`.
- `./gradlew test` runs the JUnit 5 test suite.
- `./gradlew test -Pcompile-test-snippets` repeats CI's snippet-compilation mode.
- `./gradlew publishToMavenLocal` verifies Maven publication and installs the snapshot locally for integration testing.
- `./gradlew clean` removes generated build output.

## Coding Style & Naming Conventions

Follow the official Kotlin style selected by `kotlin.code.style=official`. Use four-space indentation, LF line endings, a final newline, trailing commas in multiline declarations, and idiomatic immutable collections. Use `UpperCamelCase` for classes and data classes, `lowerCamelCase` for functions and properties, and descriptive Detekt issue IDs such as `DetectProhibitedImports`. Keep package names lowercase under `com.github.kmtr.detektimportcustoms`. No standalone formatter is configured, so match the surrounding source and let compilation catch syntax issues.

## Testing Guidelines

Tests use JUnit 5, `detekt-test`, and Kotest assertions. Name test classes after the rule (`ImportCustomsRuleTest`) and use backtick-style test names that describe behavior, such as `` `reports prohibited imports` ``. Build Kotlin snippets inline, lint them with `compileAndLintWithContext`, and assert the exact findings. Add positive and negative cases for new matching behavior. There is no enforced coverage threshold; behavioral coverage is expected.

## Commit & Pull Request Guidelines

History favors short, imperative subjects and lightweight prefixes such as `fix:`, `update:`, and `build(deps):`. Keep each commit focused; dependency updates should identify the affected library. Pull requests should explain the rule behavior changed, include configuration examples when syntax changes, and link relevant issues. Ensure `test`, snippet compilation, publication, and wrapper-validation CI checks pass before merge.
