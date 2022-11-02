[//]: # (title: Overview)

Dokka is an API documentation engine for Kotlin.

Just like Kotlin itself, Dokka supports mixed-language projects: it understands Kotlin's 
[KDoc comments](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) and Java's 
[Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).

Dokka can generate documentation in multiple formats, including its own and modern [HTML format](html.md),
multiple flavours of [Markdown](markdown.md) and Java's [Javadoc HTML](javadoc.md).

Libraries that use Dokka for API reference docs:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/)
* [Bitmovin](https://cdn.bitmovin.com/player/android/3/docs/index.html)
* [Hexagon](https://hexagonkt.com/api/index.html)
* [Ktor](https://api.ktor.io/)
* [OkHttp](https://square.github.io/okhttp/4.x/okhttp/okhttp3/) (Markdown)

Dokka can be run via [Gradle](gradle.md), [Maven](maven.md) or [command line](cli.md). It is also 
[highly pluggable](plugins_introduction.md).

## Quickstart

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

Apply Dokka Gradle plugin in the root project:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

When documenting [multi-project](gradle.md#multi-project-builds) builds, you need to apply Dokka in subprojects as well:

```kotlin
subprojects {
    apply(plugin = "org.jetbrains.dokka")
}
```

To generate documentation run the following Gradle tasks:

* `dokkaHtml` for single-project builds.
* `dokkaHtmlMultiModule` for multi-module builds.

By default, output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`. 

Learn more about Gradle configuration in a separate [topic dedicated to Gradle](gradle.md).

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

Apply Dokka Gradle plugin in the root project:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

When documenting [multi-project](gradle.md#multi-project-builds) builds, you need to apply Dokka in subprojects as well:

```groovy
subprojects {
    apply plugin: 'org.jetbrains.dokka'
}
```

To generate documentation run the following Gradle tasks:

* `dokkaHtml` for single-project builds.
* `dokkaHtmlMultiModule` for multi-module builds.

By default, output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`.

Learn more about Gradle configuration in a separate [topic dedicated to Gradle](gradle.md).

</tab>
<tab title="Maven" group-key="mvn">

Add Dokka Maven plugin to the plugins section of your POM:

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

To generate documentation run `dokka:dokka` goal. 

By default, output directory is set to `target/dokka`.

Learn more about Maven configuration in a separate [topic dedicated to Maven](maven.md).

</tab>
</tabs>
