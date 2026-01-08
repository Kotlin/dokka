# GFM plugin

> The following applies to Dokka Gradle plugin (DGP) v2 mode. The DGP v1 mode is no longer supported.
> To upgrade from v1 to v2 mode, follow the [Migration guide](dokka-migration.md).

The GFM plugin adds the ability to generate documentation in the [GitHub Flavored Markdown](https://github.github.com/gfm/) format. 
It supports both multi-project builds and Kotlin Multiplatform projects.

This format gives you more flexibility when hosting documentation, as the output can be embedded directly into GitHub-hosted 
documentation, such as GitHub Pages or repository README files.

## Gradle

The GFM format is implemented as a Dokka plugin. To properly integrate it with the [Dokka Gradle plugin](dokka-gradle.md),
create a Dokka Format Gradle plugin:

```kotlin
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPlugin.DokkaFormatPluginContext.configure() {
        project.dependencies {
            // Sets up generation for the current project
            dokkaPlugin(dokka("gfm-plugin"))

            // Sets up multi-project generation
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

            // Sets up multi-project generation
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


## Maven

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

After configuring this, run the `dokka:dokka` goal to produce documentation in GFM format.

For more information, see the Maven plugin documentation for [Other output formats](dokka-maven.md#other-output-formats).

You can find the GFM plugin on
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin).

## CLI

Since the GFM format is implemented as a [Dokka plugin](dokka-plugins.md#apply-dokka-plugins), you need to
[download the JAR file](https://repo1.maven.org/maven2/org/jetbrains/dokka/gfm-plugin/%dokkaVersion%/gfm-plugin-%dokkaVersion%.jar)
and pass it to `pluginsClasspath`.

Via [command line options](dokka-cli.md#run-with-command-line-options):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./gfm-plugin-%dokkaVersion%.jar" \
     ...
```

Or via [JSON configuration](dokka-cli.md#run-with-json-configuration):

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