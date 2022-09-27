# Dokka 

`Dokka` is an API documentation engine for `Kotlin` that performs the same function as the `Javadoc` tool for `Java`,
but it's modern and highly pluggable.

Just like `Kotlin` itself, `Dokka` supports mixed-language projects (`Kotlin`/`Java`). It understands
[KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in `Kotlin` source files as well
as [Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html#format) in `Java` 
files, and can generate documentation in multiple formats including its own `HTML` format, Java's `Javadoc` lookalike
and `Markdown`.

Some libraries that use `Dokka` for API reference docs:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/)
* [kotlinx.serialization](https://kotlinlang.org/api/kotlinx.serialization/)
* [Ktor](https://api.ktor.io/)
* [Spring Framework](https://docs.spring.io/spring-framework/docs/current/kdoc-api/)

___

`Dokka` provides support for the following build systems:

* [Gradle](user_guide/applying/gradle.md) (preffered)
* [Maven](user_guide/applying/maven.md)
* [Command line](user_guide/applying/cli.md)

___

`Dokka` is also very pluggable and comes with convenient plugin and extension point API. 

You can write a plugin to support [mermaid.js](community/plugins-list.md#mermaid) diagrams,
[mathjax](community/plugins-list.md#mathjax) formulas or even write custom processing of your own tags and annotations.

For more info, see:

* [Sample plugin tutorial](developer_guide/plugin-development/sample-plugin-tutorial.md)
* [Community plugins](community/plugins-list.md)
* [Developer guides](developer_guide/introduction.md)
