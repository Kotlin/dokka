# Dokka 

[![JetBrains official project](https://jb.gg/badges/official.svg)](https://github.com/JetBrains#jetbrains-on-github)
[![Kotlin Beta](https://kotl.in/badges/beta.svg)](https://kotlinlang.org/docs/components-stability.html)

Dokka is an API documentation engine for Kotlin.

Just like Kotlin itself, Dokka supports mixed-language projects: it understands Kotlin's
[KDoc comments](https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax) and Java's
[Javadoc comments](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).

Dokka can generate documentation in multiple formats, including its own and modern [HTML format](TODO),
multiple flavours of [Markdown](TODO) and Java's [Javadoc HTML](TODO).

Libraries that use Dokka for API reference docs:

* [kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/)
* [Bitmovin](https://cdn.bitmovin.com/player/android/3/docs/index.html)
* [Hexagon](https://hexagonkt.com/api/index.html)
* [Ktor](https://api.ktor.io/)
* [OkHttp](https://square.github.io/okhttp/4.x/okhttp/okhttp3/) (Markdown format)

Dokka can be run via [Gradle](TODO), [Maven](TODO) or [command line](TODO). It is also
[highly pluggable](TODO).

## Quickstart

### Gradle

<details open>
<summary>Kotlin DSL</summary>

Apply Dokka Gradle plugin in the root project:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.7.20"
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

By default, output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule` respectively.

</details>

<details>
<summary>Groovy DSL</summary>

Apply Dokka Gradle plugin in the root project:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '1.7.20'
}
```

When documenting multi-project builds, you need to apply Dokka in subprojects as well:

```groovy
subprojects {
    apply plugin: 'org.jetbrains.dokka'
}
```

To generate documentation run the following Gradle tasks:

* `dokkaHtml` for single-project builds.
* `dokkaHtmlMultiModule` for multi-module builds.

By default, output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`.

</details>

Learn more about Gradle configuration in a separate [topic dedicated to Gradle](TODO). 

### Maven

Add Dokka Maven plugin to the plugins section of your POM:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.dokka</groupId>
            <artifactId>dokka-maven-plugin</artifactId>
            <version>1.7.20</version>
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

To generate documentation run `dokka:dokka` goal. By default, output directory is set to `target/dokka`.

Learn more about Maven configuration in a separate [topic dedicated to Maven](TODO).

### CLI

It is possible to run Dokka from command line without having to use any of the build tools, but it's more
difficult to set up and for that reason is not covered by this Quickstart guide.

Please consult [documentation for command line runner](https://kotlin.github.io/dokka/1.7.20/user_guide/applying/cli/)
to learn how to use it.

### Android

In addition to applying and configuring Dokka, you can apply Dokka's 
[Android documentation plugin](plugins/android-documentation) which aims to improve documentation experience on the 
Android platform:

<details open>
<summary>Gradle Kotlin DSL</summary>

```kotlin
dependencies {
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:1.7.20")
}
```

</details>

<details>
<summary>Gradle Groovy DSL</summary>

```groovy
dependencies {
    dokkaPlugin 'org.jetbrains.dokka:android-documentation-plugin:1.7.20'
}
```

</details>

<details>
<summary>Maven</summary>

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>android-documentation-plugin</artifactId>
                <version>1.7.20</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

</details>

## Output formats

### HTML

HTML is Dokka's default, stable and recommended format. You can see it in action by browsing documentation for 
[kotlinx.coroutines](https://kotlinlang.org/api/kotlinx.coroutines/).

HTML format is configurable and, among other things, allows you to modify stylesheets, add custom image assets, change
footer message and revamp the structure of generated HTML pages through templates.

For more details and examples see [HTML format](TODO) topic.

### Markdown

Dokka is able to generate documentation in GitHub Flavored and Jekyll compatible Markdown. However, both of these
formats are still in Alpha, so you might encounter bugs and migration issues.

`GFM` and `Jekyll` formats are essentially Dokka plugins. Learn how to apply and use them in a separate topic
dedicated to [Markdown format](TODO).

### Javadoc

Dokka's Javadoc output format is a lookalike of Java's 
[Javadoc HTML format](https://docs.oracle.com/en/java/javase/19/docs/api/index.html). The format is still in Alpha,
so you might encounter bugs and migration issues.

Javadoc format tries to visually mimic HTML pages generated by the Javadoc tool, but it's not a direct implementation, 
and it's not an exact copy. In addition, all Kotlin signatures are translated to Java signatures.

For more details and examples see [Javadoc format](TODO) topic.

## Dokka plugins

Dokka was built to be easily extensible and highly customizable, which allows the community to implement plugins for 
missing or very specific features that are not provided out of the box.

Learn more about Dokka plugin and their configuration in a separate [topic dedicated to plugins](TODO)

If you want to learn how to create Dokka plugins, see
[Developer guides](https://kotlin.github.io/dokka/1.7.20/developer_guide/introduction/).

## Building and Contributing

See [Contributing Guidelines](CONTRIBUTING.md)
