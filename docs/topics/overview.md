[//]: # (title: Overview)

Dokka is an API documentation engine for Kotlin.

Just like Kotlin itself, Dokka supports mixed-language projects. It understands Kotlin's
[KDoc comments](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) and Java's 
[Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).

Dokka can generate documentation in multiple formats, including its own modern [HTML format](html.md),
multiple flavors of [Markdown](markdown.md), and Java's [Javadoc HTML](javadoc.md).

There are already a number of libraries that use Dokka for their API reference documentation:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/)
* [Bitmovin](https://cdn.bitmovin.com/player/android/3/docs/index.html)
* [Hexagon](https://hexagonkt.com/api/index.html)
* [Ktor](https://api.ktor.io/)
* [OkHttp](https://square.github.io/okhttp/4.x/okhttp/okhttp3/) (Markdown)

Dokka can be run via [Gradle](gradle.md), [Maven](maven.md) or on the [command line](cli.md). It is also
[highly pluggable](plugins_introduction.md).

## Quickstart

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

Apply the Dokka Gradle plugin in the root of your project:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

When documenting [multi-project](gradle.md#multi-project-builds) builds, you need to apply the Dokka plugin within subprojects as well:

```kotlin
subprojects {
    apply(plugin = "org.jetbrains.dokka")
}
```

To generate documentation, run the following Gradle tasks:

* `dokkaHtml` for single-project builds
* `dokkaHtmlMultiModule` for multi-module builds

By default, the output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`. 

To learn more about Gradle configuration, see [Gradle](gradle.md).

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

Apply the Dokka Gradle plugin in the root of your project:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

When documenting [multi-project](gradle.md#multi-project-builds) builds, you need to apply the Dokka plugin within subprojects as well:

```groovy
subprojects {
    apply plugin: 'org.jetbrains.dokka'
}
```

To generate documentation, run the following Gradle tasks:

* `dokkaHtml` for single-project builds
* `dokkaHtmlMultiModule` for multi-module builds

By default, output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`.

To learn more about Gradle configuration, see [Gradle](gradle.md).

</tab>
<tab title="Maven" group-key="mvn">

Add the Dokka Maven plugin to the plugins section of your POM file:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.dokka</groupId>
            <artifactId>dokka-maven-plugin</artifactId>
            <version>%dokkaVersion%</version>
            <executions>
                <execution>
                    <phase>pre-site</phase>
                    <goals>
                        <goal>dokka</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

To generate documentation, run the `dokka:dokka` goal. 

By default, the output directory is set to `target/dokka`.

To learn more about Maven configuration, see [Maven](maven.md).

</tab>
</tabs>

## Community

Dokka has a dedicated `#dokka` channel in [Kotlin Community Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)
where you can chat about Dokka, its plugins and how to develop them, as well as get in touch with maintainers.
