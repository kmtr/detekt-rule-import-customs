# Detekt Import Customs

com.example.package1
com.example.package2
com.example.package3

You can add a new restriction like following.

`com.example.package1` must not import `com.example.package2` and `com.example.package3`.

com.example.package1
prohibited packages
    com.example.package2
    com.example.package3

## Usecase

com.example.logic
com.example.web
com.example.db

`logic` must not import `web`, `db`

## Ref

https://detekt.dev/docs/introduction/extensions/
https://github.com/detekt/detekt-custom-rule-template
https://github.com/detekt/detekt/tree/main/detekt-sample-extensions
