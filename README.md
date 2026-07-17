# Detekt Import Customs

Detekt Import Customs adds directional import restrictions to Kotlin projects.
Each restriction selects source packages and reports imports of forbidden packages.

## Installation

Build the plugin and add the resulting JAR to the consuming project's Detekt
plugins configuration:

```kotlin
dependencies {
    detektPlugins(files("path/to/detekt-import-customs-1.0-SNAPSHOT.jar"))
}
```

The JAR is created under `build/libs/` by `mise run build`.

## Configuration

Rule set and rule IDs must match `ImportCustoms` and
`DetectProhibitedImports` respectively:

```yaml
ImportCustoms:
  DetectProhibitedImports:
    active: true
    patterns:
      - '^com\.example\.pattern(?:\..*)?::^com\.example\.package2(?:\..*)?,^com\.example\.lib\.package3(?:\..*)?'
```

The part before `::` selects source packages. Comma-separated expressions
after `::` select forbidden imports. Expressions are Kotlin regular
expressions and match the complete package or import name.

The example prevents `com.example.pattern` and its subpackages from importing
anything in `com.example.package2` or `com.example.lib.package3`.

## Development

The required Temurin JDK and Gradle versions are managed with
[mise](https://mise.jdx.dev/). Install the toolchain and run the checks with:

```shell
mise install
mise run test
mise run build
```

Run `mise tasks` to list all available project tasks. The configured Gradle
version matches the checked-in Gradle Wrapper used by CI.
