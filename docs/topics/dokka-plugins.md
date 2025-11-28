[//]: # (title: Dokka plugins)

> This guide applies to Dokka Gradle plugin (DGP) v2 mode. The previous DGP v1 mode is no longer supported.
> If you're upgrading from v1 to v2 mode, see the [Migration guide](dokka-migration.md).
>
{style="note"}

Dokka was built from the ground up to be easily extensible and highly customizable, which allows the community
to implement plugins for missing or very specific features that are not provided out of the box.

Dokka plugins range anywhere from supporting other programming language sources to exotic output formats. You can add
support for your own KDoc tags or annotations, teach Dokka how to render different DSLs that are found in KDoc
descriptions, visually redesign Dokka's pages to be seamlessly integrated into your company's website, integrate it
with other tools and so much more. 

If you want to learn how to create Dokka plugins, see 
[Developer guides](https://kotlin.github.io/dokka/%dokkaVersion%/developer_guide/introduction/).

## Apply Dokka plugins

Dokka plugins are published as separate artifacts, so to apply a Dokka plugin, you only need to add it as a dependency. 
From there, the plugin extends Dokka by itself—no further action is needed.

> Plugins that use the same extension points or work in a similar way can interfere with each other.
> This may lead to visual bugs, general undefined behavior or even failed builds. However, it should not lead to 
> concurrency issues since Dokka does not expose any mutable data structures or objects.
>
> If you notice problems like this, it's a good idea to check which plugins are applied and what they do.
> 
{style="note"}

Let's have a look at how you can apply the [mathjax plugin](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-mathjax)
to your project:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

The way to apply Dokka plugins is:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin")
}
```

> * Built-in plugins (like HTML and Javadoc) are always applied automatically. You only configure them and do not need dependencies for them.
>
> * When documenting multi-module projects (multi-project builds), you need to [share Dokka configuration and plugins across subprojects](dokka-gradle.md#multi-project-configuration).
> 
{style="note"}

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}

dependencies {
    dokkaPlugin 'org.jetbrains.dokka:mathjax-plugin'
}
```

> When documenting [multi-project](dokka-gradle.md#multi-project-configuration) builds, 
> you need to [share Dokka configuration across subprojects](dokka-gradle.md#multi-project-configuration).
>
{style="note"}

</tab>
<tab title="Maven" group-key="mvn">

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>mathjax-plugin</artifactId>
                <version>%dokkaVersion%</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

</tab>
<tab title="CLI" group-key="cli">

If you are using the [CLI](dokka-cli.md) runner with [command line options](dokka-cli.md#run-with-command-line-options), 
Dokka plugins should be passed as `.jar` files to `-pluginsClasspath`:

```Shell
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./mathjax-plugin-%dokkaVersion%.jar" \
     ...
```

If you are using [JSON configuration](dokka-cli.md#run-with-json-configuration), Dokka plugins should be specified under 
`pluginsClasspath`.

```json
{
  ...
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "...",
    "./mathjax-plugin-%dokkaVersion%.jar"
  ],
  ...
}
```

</tab>
</tabs>

## Configure Dokka plugins

Dokka plugins can also have configuration options of their own. To see which options are available, consult
the documentation of the plugins you are using. 

Let's have a look at how you can configure the built-in HTML plugin by adding a custom image to the assets 
(`customAssets` option), 
custom style sheets (`customStyleSheets` option), and a modified footer message (`footerMessage` option):

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

To configure Dokka plugins in a type-safe way, use the `dokka.pluginsConfiguration {}` block:

```kotlin
dokka {
    pluginsConfiguration.html {
        customAssets.from("logo.png")
        customStyleSheets.from("styles.css")
        footerMessage.set("(c) Your Company")
    }
}
```

For an example of Dokka plugins configuration, see the
[Dokka's versioning plugin](https://github.com/Kotlin/dokka/tree/master/examples/gradle-v2/versioning-multimodule-example).

Dokka allows you 
to extend its functionality 
by [configuring custom plugins](https://github.com/Kotlin/dokka/blob/v2.1.0/examples/gradle-v2/custom-dokka-plugin-example/demo-library/build.gradle.kts).
Custom plugins enable additional processing or modifications to the documentation generation process.

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
dokka {
    pluginsConfiguration {
        html {
            customAssets.from("logo.png")
            customStyleSheets.from("styles.css")
            footerMessage.set("(c) Your Company")
        }
    }
}
```

</tab>
<tab title="Maven" group-key="mvn">

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <pluginsConfiguration>
            <!-- Fully qualified plugin name -->
            <org.jetbrains.dokka.base.DokkaBase>
                <!-- Options by name -->
                <customAssets>
                    <asset>${project.basedir}/my-image.png</asset>
                </customAssets>
                <customStyleSheets>
                    <stylesheet>${project.basedir}/my-styles.css</stylesheet>
                </customStyleSheets>
                <footerMessage>(c) MyOrg 2022 Maven</footerMessage>
            </org.jetbrains.dokka.base.DokkaBase>
        </pluginsConfiguration>
    </configuration>
</plugin>
```

</tab>
<tab title="CLI" group-key="cli">

If you are using the [CLI](dokka-cli.md) runner with [command line options](dokka-cli.md#run-with-command-line-options),
use the `-pluginsConfiguration` option that accepts JSON configuration in the form of `fullyQualifiedPluginName=json`.

If you need to configure multiple plugins, you can pass multiple values separated by `^^`.

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     ...
     -pluginsConfiguration "org.jetbrains.dokka.base.DokkaBase={\"customAssets\": [\"my-image.png\"], \"customStyleSheets\": [\"my-styles.css\"], \"footerMessage\": \"(c) 2022 MyOrg CLI\"}"
```

If you are using [JSON configuration](dokka-cli.md#run-with-json-configuration), there exists a similar
`pluginsConfiguration` array that accepts JSON configuration in `values`.

```json
{
  "moduleName": "Dokka Example",
  "pluginsConfiguration": [
    {
      "fqPluginName": "org.jetbrains.dokka.base.DokkaBase",
      "serializationFormat": "JSON",
      "values": "{\"customAssets\": [\"my-image.png\"], \"customStyleSheets\": [\"my-styles.css\"], \"footerMessage\": \"(c) 2022 MyOrg\"}"
    }
  ]
}
```

</tab>
</tabs>

## Notable plugins

Here are some notable Dokka plugins that you might find useful:

| **Name**                                                                                                                           | **Description**                                                                                              |
|------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| [Android documentation plugin](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-android-documentation) | Improves the documentation experience on Android                                                             |
| [Versioning plugin](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-versioning)                       | Adds version selector and helps to organize documentation for different versions of your application/library |
| [MermaidJS HTML plugin](https://github.com/glureau/dokka-mermaid)                                                                  | Renders [MermaidJS](https://mermaid-js.github.io/mermaid/#/) diagrams and visualizations found in KDocs      |
| [Mathjax HTML plugin](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-mathjax)                        | Pretty prints mathematics found in KDocs                                                                     |
| [Kotlin as Java plugin](https://github.com/Kotlin/dokka/tree/%dokkaVersion%/dokka-subprojects/plugin-kotlin-as-java)               | Renders Kotlin signatures as seen from Java's perspective                                                    |
| [GFM plugin](https://github.com/Kotlin/dokka/tree/master/dokka-subprojects/plugin-gfm)                                                                                                                     | Adds the ability to generate documentation in GitHub Flavoured Markdown format                               |
| [Jekyll plugin](https://github.com/Kotlin/dokka/tree/master/dokka-subprojects/plugin-jekyll)                                                                                                                                                                                                           | Adds the ability to generate documentation in Jekyll Flavoured Markdown format                               |

If you are a Dokka plugin author and would like to add your plugin to this list, get in touch with maintainers
via [Slack](dokka-introduction.md#community) or [GitHub](https://github.com/Kotlin/dokka/).
