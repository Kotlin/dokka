# Jekyll plugin

> The following applies to Dokka Gradle plugin (DGP) v2 mode. The DGP v1 mode is no longer supported.
> To upgrade from v1 to v2 mode, follow the [Migration guide](dokka-migration.md).

The Jekyll plugin adds the ability to generate documentation in [Jekyll](https://jekyllrb.com/) Flavoured Markdown format. 
It supports both
multi-project builds and Kotlin Multiplatform projects.

**This plugin is at its early stages**, so you may experience issues and encounter bugs. Feel free to
[report](https://github.com/Kotlin/dokka/issues/new/choose) any errors you see.

## Gradle

The [Gradle plugin for Dokka](dokka-gradle.md) comes with the Jekyll output format included.

You need to
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
            // Sets up generation for the current project
            dokkaPlugin(dokka("jekyll-plugin"))

            // Sets up Multi-project generation
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

## Maven

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

After configuring this, run the `dokka:dokka` goal to produce documentation in Jekyll format.

For more information, see the Maven plugin's documentation for [Other output formats](dokka-maven.md#other-output-formats).
You can find the Jekyll plugin on
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/jekyll-plugin).

## CLI

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

Or via [JSON configuration](dokka-cli.md#run-with-json-configuration):

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

You can find the source code on [GitHub](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-jekyll).