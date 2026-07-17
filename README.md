# Detekt Import Customs

Detekt Import Customs adds directional dependency restrictions to Kotlin
projects. Each restriction selects source packages and reports both imports and
fully qualified references to forbidden packages.

## Installation

Versioned artifacts are published to GitHub Packages. Add the repository and
plugin dependency to the consuming project:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/kmtr/detekt-rule-import-customs")
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}

dependencies {
    detektPlugins("com.github.kmtr.detektimportcustoms:detekt-import-customs:1.0.0")
}
```

GitHub Packages requires a token with package read access, including for public
packages. For local development, `mise run publish-repository` creates a Maven
repository under `build/repository`; `mise run publish-local` installs the same
artifact into Maven Local.

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

## Compatibility

This release targets Detekt 1.23.8, Kotlin 2.0.21, and JVM 1.8 bytecode. It can
therefore be loaded by Detekt processes running on JDK 8 through JDK 21. The
build itself uses JDK 21.

### JVM compatibility policy

The JDK used to build the project and the bytecode level published to users are
managed independently. Updating the development JDK does not by itself justify
raising the published JVM target. The artifact remains on JVM 1.8 while the
supported Detekt line can run on it, so consumers are not forced to upgrade
their Detekt runtime JDK.

When changing JVM compatibility:

- Keep Java `sourceCompatibility` and `targetCompatibility` aligned with
  Kotlin `jvmTarget` in `build.gradle.kts`.
- Keep the Kotlin compiler and standard library aligned with the version used
  by the supported Detekt release to avoid contaminating Detekt's runtime
  classpath with an incompatible Kotlin version.
- Raise the minimum JVM only when a required dependency or supported Detekt
  release no longer supports the current target. Treat that change as a
  consumer-facing compatibility break and document it in the release notes.
- Verify `mise run build`, `mise run test-snippets`, and
  `mise run publish-repository`, then inspect the published rule class with
  `javap -verbose` to confirm the expected class-file major version. JVM 1.8
  corresponds to major version 52.

Any JVM target change must update this section, the Gradle settings, the mise
toolchain, and CI in the same pull request.

Detekt 2.0 uses a binary-incompatible extension API and is not supported by
this artifact while the 2.0 line remains in alpha. A separate artifact or a
major release will be required for Detekt 2.0 support.

## Development

The required Temurin JDK and Gradle versions are managed with
[mise](https://mise.jdx.dev/). Install the toolchain and run the checks with:

```shell
mise install
mise run test
mise run build
mise run test-snippets
mise run publish-repository
```

Run `mise tasks` to list all available project tasks. The configured Gradle
version matches the checked-in Gradle Wrapper used by CI.
