# Kotlin as Java plugin

With Kotlin as Java plugin applied, all `Kotlin` signatures will be rendered as `Java` signatures.

For instance, `fun foo(bar: Bar): Baz` will be rendered as `public final Baz foo(Bar bar)`.

Kotlin as Java plugin is published to maven central as a
[separate artifact](https://mvnrepository.com/artifact/org.jetbrains.dokka/kotlin-as-java-plugin):
```text
org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20
```
