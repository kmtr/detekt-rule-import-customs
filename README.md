# Detekt Rule of Import Customs

You can add new import restrictions, if you want to prohibit certain imports on the specific package.
This rule gives one-way import rules.

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

## Sample

This is a sample of the configuration of this rule.

```
patterns:
    - ^com\\.example\\.pattern::^com\\.example\\.package2,^com\\.example\\.lib\\.package3.*
```

This configuration gives you a following restriction.

`com.example.pattern` must not import `com.example.package2` and `com.example.lib.package3.logic`.

TODO: improve configuration expression
