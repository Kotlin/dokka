[//]: # (title: Markdown)
<primary-label ref="alpha"/>

> This guide applies to Dokka Gradle Plugin (DGP) v2 mode. The previous DGP v1 mode is no longer supported.
> If you're upgrading from v1 to v2 mode, see the [Migration guide](dokka-migration.md).
>
{style="note"}


Dokka is able to generate documentation in [GitHub Flavored](#gfm) and [Jekyll](#jekyll) compatible Markdown.

These formats give you more freedom in terms of hosting documentation as the output can be embedded right into your 
documentation website. For example, see [OkHttp's API reference](https://square.github.io/okhttp/5.x/okhttp/okhttp3/)
pages.

Markdown output formats are implemented as [Dokka plugins](dokka-plugins.md), maintained by the Dokka team, and 
they are open source.

## GFM

The GFM output format generates documentation in [GitHub Flavored Markdown](https://github.github.com/gfm/).

<tabs group="build-script">
<tab title="Gradle" group-key="kotlin">

The GFM format is implemented as a Dokka plugin, 
but to properly integrate it with the [Dokka Gradle Plugin](dokka-gradle.md).
You need to create a Dokka Format Gradle plugin:

```kotlin
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPlugin.DokkaFormatPluginContext.configure() {
        project.dependencies {
            // Sets up current project generation
            dokkaPlugin(dokka("gfm-plugin"))

            // Sets up multimodule generation
            formatDependencies.dokkaPublicationPluginClasspathApiOnly.dependencies.addLater(
                dokka("gfm-template-processing-plugin")
            )
        }
    }
}
```

It's possible to declare the Dokka Format Gradle plugin directly in the build script, 
as a separate file in build-logic, or inside a convention plugin.

After you declared the Dokka Format Gradle plugin, 
apply it in every place where the Dokka plugin is applied, including the project with aggregation 
(such as the root project).

If you are using a convention plugin, you can add it like this:

```kotlin
plugins {
    id("org.jetbrains.dokka")
}

// Declares Markdown Gradle plugin
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPlugin.DokkaFormatPluginContext.configure() {
        project.dependencies {
            // Sets up current project generation
            dokkaPlugin(dokka("gfm-plugin"))

            // Sets up multimodule generation
            formatDependencies.dokkaPublicationPluginClasspathApiOnly.dependencies.addLater(
                dokka("gfm-template-processing-plugin")
            )
        }
    }
}
// Applies the plugin
apply<DokkaMarkdownPlugin>()
```

Once you applied the plugin, you can run the following tasks:
* `dokkaGenerate` to generate documentation in [all available formats based on the applied plugins](dokka-gradle.md#configure-documentation-output-format).
* `dokkaGenerateMarkdown` to generate documentation only in Markdown format.

</tab>
<tab title="Maven" group-key="groovy">

Since the GFM format is implemented as a [Dokka plugin](dokka-plugins.md#apply-dokka-plugins), 
you need to apply it as a plugin
dependency:

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>gfm-plugin</artifactId>
                <version>%dokkaVersion%</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

After configuring this, running the `dokka:dokka` goal produces documentation in GFM format.

For more information, see the Maven plugin documentation for [Other output formats](dokka-maven.md#other-output-formats).

</tab>
<tab title="CLI" group-key="cli">

Since the GFM format is implemented as a [Dokka plugin](dokka-plugins.md#apply-dokka-plugins), you need to 
[download the JAR file](https://repo1.maven.org/maven2/org/jetbrains/dokka/gfm-plugin/%dokkaVersion%/gfm-plugin-%dokkaVersion%.jar)
and pass it to `pluginsClasspath`.

Via [command line options](dokka-cli.md#run-with-command-line-options):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./gfm-plugin-%dokkaVersion%.jar" \
     ...
```

Via [JSON configuration](dokka-cli.md#run-with-json-configuration):

```json
{
  ...
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "...",
    "./gfm-plugin-%dokkaVersion%.jar"
  ],
  ...
}
```

For more information, see the CLI runner's documentation for [Other output formats](dokka-cli.md#other-output-formats).

</tab>
</tabs>

## Jekyll

The Jekyll output format generates documentation in [Jekyll](https://jekyllrb.com/) compatible Markdown.

<tabs group="build-script">
<tab title="Gradle" group-key="kotlin">

The [Gradle plugin for Dokka](dokka-gradle.md) comes with the Jekyll output format included. 

Similar to [GFM](#gfm), you need to
declare the Dokka Format Gradle plugin directly in the build script, 
as a separate file in build-logic, or inside a convention plugin.
Then, apply it in every place where the Dokka plugin is applied:

```kotlin
plugins {
    id("org.jetbrains.dokka")
}

// Declares Markdown Gradle plugin
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPlugin.DokkaFormatPluginContext.configure() {
        project.dependencies {
            // Sets up current project generation
            dokkaPlugin(dokka("jekyll-plugin"))

            // Sets up multimodule generation
            formatDependencies.dokkaPublicationPluginClasspathApiOnly.dependencies.addLater(
                dokka("jekyll-template-processing-plugin")
            )
        }
    }
}
// Applies the plugin
apply<DokkaMarkdownPlugin>()
```

Once you applied the plugin, you can run the following tasks:
* `dokkaGenerateMarkdown` to generate documentation only in Markdown format.
* `dokkaGenerate` to generate documentation in [all available formats based on the applied plugins](dokka-gradle.md#configure-documentation-output-format).

</tab>
<tab title="Maven" group-key="groovy">

Since the Jekyll format is implemented as a [Dokka plugin](dokka-plugins.md#apply-dokka-plugins), 
you need to apply it as a plugin
dependency:

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>jekyll-plugin</artifactId>
                <version>%dokkaVersion%</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

After configuring this, running the `dokka:dokka` goal produces documentation in GFM format.

For more information, see the Maven plugin's documentation for [Other output formats](dokka-maven.md#other-output-formats).

</tab>
<tab title="CLI" group-key="cli">

Since the Jekyll format is implemented as a [Dokka plugin](dokka-plugins.md#apply-dokka-plugins), you need to 
[download the JAR file](https://repo1.maven.org/maven2/org/jetbrains/dokka/jekyll-plugin/%dokkaVersion%/jekyll-plugin-%dokkaVersion%.jar).
This format is also based on [GFM](#gfm) format, so you need to provide it as a dependency as well. Both JARs need to be passed to 
`pluginsClasspath`:

Via [command line options](dokka-cli.md#run-with-command-line-options):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./gfm-plugin-%dokkaVersion%.jar;./jekyll-plugin-%dokkaVersion%.jar" \
     ...
```

Via [JSON configuration](dokka-cli.md#run-with-json-configuration):

```json
{
  ...
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "...",
    "./gfm-plugin-%dokkaVersion%.jar",
    "./jekyll-plugin-%dokkaVersion%.jar"
  ],
  ...
}
```

For more information, see the CLI runner's documentation for [Other output formats](dokka-cli.md#other-output-formats).

</tab>
</tabs>

You can find the source code on [GitHub](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-jekyll).
