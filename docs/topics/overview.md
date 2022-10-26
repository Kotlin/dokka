[//]: # (title: Overview)

Dokka is an API documentation engine for Kotlin.

Just like Kotlin itself, Dokka supports mixed-language projects: it understands Kotlin's 
[KDoc comments](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) and Java's 
[Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).

Dokka can generate documentation in multiple formats, including its own and modern [HTML format](html.md),
multiple flavours of [Markdown](markdown.md) and Java's [Javadoc HTML](javadoc.md).

Libraries that use Dokka for API reference docs:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/)
* [detekt](https://detekt.dev/kdoc/detekt-api/io.gitlab.arturbosch.detekt.api/index.html)
* [Ktor](https://api.ktor.io/)
* [Spring Framework](https://docs.spring.io/spring-framework/docs/current/kdoc-api/)
* [OkHttp](https://square.github.io/okhttp/4.x/okhttp/okhttp3/) (Markdown)

Dokka can be run via [Gradle](gradle.md), [Maven](maven.md) or [command line](cli.md). It is also 
[highly pluggable](plugins_introduction.md).
