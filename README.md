# Detekt Import Customs

Detekt Import Customs adds directional dependency restrictions to Kotlin
projects. Each restriction selects source packages and reports both imports and
fully qualified references to forbidden packages.

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
    restrictions:
      - from: '^com\.example\.pattern(?:\..*)?'
        disallow:
          - '^com\.example\.package2(?:\..*)?'
          - '^com\.example\.lib\.package3(?:\..*)?'
        allow:
          - '^com\.example\.package2\.publicapi(?:\..*)?'
        reason: 'Depend on the domain API instead.'
```

Each restriction supports the following properties:

- `from` selects source packages.
- `disallow` contains forbidden import patterns and must not be empty.
- `allow` optionally makes exceptions to `disallow`.
- `reason` optionally adds migration guidance to findings.

Expressions are Kotlin regular expressions and match the complete package or
import name. Configuration is validated when Detekt creates the rule, so
malformed entries and invalid expressions fail with the property path.

The example prevents `com.example.pattern` and its subpackages from importing
anything in `com.example.package2` or `com.example.lib.package3`, except for
the explicitly allowed `com.example.package2.publicapi` package. Using a fully
qualified reference instead of an import is subject to the same restriction.

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
