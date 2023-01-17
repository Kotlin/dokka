[//]: # (title: Introduction)

Dokka is an API documentation engine for Kotlin.

Just like Kotlin itself, Dokka supports mixed-language projects. It understands Kotlin's
[KDoc comments](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) and Java's 
[Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).

Dokka can generate documentation in multiple formats, including its own modern [HTML format](dokka-html.md),
multiple flavors of [Markdown](dokka-markdown.md), and Java's [Javadoc HTML](dokka-javadoc.md).

Here are some libraries that use Dokka for their API reference documentation:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/)
* [Bitmovin](https://cdn.bitmovin.com/player/android/3/docs/index.html)
* [Hexagon](https://hexagonkt.com/api/index.html)
* [Ktor](https://api.ktor.io/)
* [OkHttp](https://square.github.io/okhttp/4.x/okhttp/okhttp3/) (Markdown)

You can run Dokka using [Gradle](dokka-gradle.md), [Maven](dokka-maven.md) or from the [command line](dokka-cli.md). It is also
[highly pluggable](dokka-plugins.md).

See [Get started with Dokka](dokka-get-started.md) to take your first steps in using Dokka.

## Community

Dokka has a dedicated `#dokka` channel in [Kotlin Community Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)
where you can chat about Dokka, its plugins and how to develop them, as well as get in touch with maintainers.
