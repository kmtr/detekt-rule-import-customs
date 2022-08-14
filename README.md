# Detekt Rule of Import Customs

You can add new import restrictions, if you want to prohibit certain imports on the specific package.
This rule gives one-way import rules.

## Sample

This is a sample of the configuration of this rule.

```
patterns:
    - ^com\\.example\\.pattern::^com\\.example\\.package2,^com\\.example\\.lib\\.package3.*
```

This configuration gives you a following restriction.

`com.example.pattern` must not import `com.example.package2` and `com.example.lib.package3.logic`.

TODO: improve configuration expression
