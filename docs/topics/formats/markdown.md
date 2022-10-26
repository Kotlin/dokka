[//]: # (title: Markdown)

> Markdown output formats are still in Alpha, use at your own risk, expect bugs and migration issues.
>
{type="warning"}

Dokka is able to generate documentation in [GitHub Flavored](#gfm) and [Jekyll](#jekyll) compatible Markdown.

These formats give you more freedom in terms of hosting documentation as the output can be embedded right into your 
documentation website. For example, see [OkHttp's API reference](https://square.github.io/okhttp/4.x/okhttp/okhttp3/)
pages.

Markdown output formats are rendering [Dokka plugin](plugins_introduction.md), maintained by the Dokka team, and 
they are open source.

## GFM

GFM format generates documentation in [GitHub Flavored Markdown](https://github.github.com/gfm/).

<tabs group="build-script">
<tab title="Gradle" group-key="kotlin">

Dokka's [Gradle plugin](gradle.md) ships with GFM format included, you can use the following tasks:

| **Task**              | **Description**                                                                                                                                                                                                                    |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dokkaGfm`            | Generates GFM documentation for a single project.                                                                                                                                                                                  |
| `dokkaGfmMultiModule` | A [MultiModule](gradle.md#multi-project-builds) task created only for parent projects in multi-project builds. Generates documentation for subprojects and collects all outputs in a single place with a common table of contents. |
| `dokkaGfmCollector`   | A [Collector](gradle.md#collector-tasks) task created only for parent projects in multi-project builds. Calls `dokkaGfm` for every subproject and merges all outputs into a single virtual project.                                |

</tab>
<tab title="Maven" group-key="groovy">

Since GFM format is a [Dokka plugin](plugins_introduction.md#applying-dokka-plugins), you need to apply it as a plugin
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

After that, running `dokka:dokka` goal should produce documentation in GFM format.

For more information, see [other output formats](maven.md#other-output-formats) section for the Maven plugin.

</tab>
<tab title="CLI" group-key="cli">

Since GFM format is a [Dokka plugin](plugins_introduction.md#applying-dokka-plugins), you need to download the
[jar file](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin/%dokkaVersion%) and pass it to
`pluginsClasspath`.

Via [command line arguments](cli.md#running-with-command-line-arguments):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./gfm-plugin-%dokkaVersion%.jar" \
     ...
```

Via [JSON configuration](cli.md#running-with-json-configuration):

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

For more information, see [other output formats](cli.md#other-output-formats) section for the CLI runner.

</tab>
</tabs>

You can find sources [on GitHub](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/plugins/gfm).

## Jekyll

Jekyll format generates documentation in [Jekyll](https://jekyllrb.com/) compatible Markdown.

<tabs group="build-script">
<tab title="Gradle" group-key="kotlin">

Dokka's [Gradle plugin](gradle.md) ships with Jekyll format included, you can use the following tasks:

| **Task**                 | **Description**                                                                                                                                                                                                                    |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dokkaJekyll`            | Generates Jekyll documentation for a single project.                                                                                                                                                                               |
| `dokkaJekyllMultiModule` | A [MultiModule](gradle.md#multi-project-builds) task created only for parent projects in multi-project builds. Generates documentation for subprojects and collects all outputs in a single place with a common table of contents. |
| `dokkaJekyllCollector`   | A [Collector](gradle.md#collector-tasks) task created only for parent projects in multi-project builds. Calls `dokkaJekyll` for every subproject and merges all outputs into a single virtual project.                             |

</tab>
<tab title="Maven" group-key="groovy">

Since Jekyll format is a [Dokka plugin](plugins_introduction.md#applying-dokka-plugins), you need to apply it as a plugin
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

After that, running `dokka:dokka` goal should produce documentation in GFM format.

For more information, see [other output formats](maven.md#other-output-formats) section for the Maven plugin.

</tab>
<tab title="CLI" group-key="cli">

Since Jekyll format is a [Dokka plugin](plugins_introduction.md#applying-dokka-plugins), you need to download the
[jar file](https://mvnrepository.com/artifact/org.jetbrains.dokka/jekyll-plugin/%dokkaVersion%). This format is also
based on [GFM](#gfm) format, so you need to provide it as a dependency as well. Both jars need to be passed to 
`pluginsClasspath`:

Via [command line arguments](cli.md#running-with-command-line-arguments):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./gfm-plugin-%dokkaVersion%.jar;./jekyll-plugin-%dokkaVersion%.jar" \
     ...
```

Via [JSON configuration](cli.md#running-with-json-configuration):

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

For more information, see [other output formats](cli.md#other-output-formats) section for the CLI runner.

</tab>
</tabs>

You can find sources [on GitHub](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/plugins/jekyll).
