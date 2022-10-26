[//]: # (title: Dokka plugins)

Dokka was built from the ground up to be easily extensible and highly customizable, which allows the community
to implement plugins for missing or very specific features that are not provided out of the box.

Dokka plugins range anywhere from supporting other programming language sources to exotic output formats. You can add
support for your own KDoc tags or annotations, teach Dokka how to render different DSLs that are found in KDoc
descriptions, visually redesign Dokka's pages to be seamlessly integrated into your company's website, integrate it
with other tools and so much more. 

If you want to learn how to create Dokka plugins, see 
[Developer guides](https://kotlin.github.io/dokka/%dokkaVersion%/developer_guide/introduction/).

## Applying Dokka plugins

Dokka plugins are published as separate artifacts, so to apply a Dokka plugin you only need to add as a dependency.
From there, the plugin will extend Dokka by itself - no extra actions needed. 

> Plugins that use the same extension points or work in a similar way can interfere with each other.
> This may lead to visual bugs, general undefined behaviour or even failed builds. If you notice anything of that
> nature, it's a good idea to check which plugins are applied and what they do.
> 
{type="note"}

Let's have a look at how you can apply [mathjax plugin](https://github.com/Kotlin/dokka/tree/master/plugins/mathjax)
to your project:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

Gradle plugin creates convenient dependency configurations that allow you to apply plugins universally or
for a specific output format only.

```kotlin
dependencies {
    // will be applied universally
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin:%dokkaVersion%")
    
    // will be applied for single-module dokkaHtml task only
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:%dokkaVersion%")

    // will be applied for html format in multi-project builds
    dokkaHtmlPartialPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:%dokkaVersion%")
}
```

</tab>
<tab title="Groovy" group-key="groovy">

Gradle plugin creates convenient dependency configurations that allow you to apply Dokka plugins universally or
for a specific output format only.

```groovy
dependencies {
    // will be applied universally
    dokkaPlugin 'org.jetbrains.dokka:mathjax-plugin:%dokkaVersion%'

    // will be applied for single-module dokkaHtml task only
    dokkaHtmlPlugin 'org.jetbrains.dokka:kotlin-as-java-plugin:%dokkaVersion%'

    // will be applied for html format in multi-project builds
    dokkaHtmlPartialPlugin 'org.jetbrains.dokka:kotlin-as-java-plugin:%dokkaVersion%'
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

If you are using the [CLI](cli.md) runner with [command line arguments](cli.md#running-with-command-line-arguments), 
Dokka plugins should be passed as `.jar` files to `-pluginsClasspath`:

```Shell
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./mathjax-plugin-%dokkaVersion%.jar" \
     ...
```

If you are using [JSON configuration](cli.md#running-with-json-configuration), Dokka plugins should be specified under 
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

## Configuring Dokka plugins

Dokka plugins can also have configuration options of their own. Consult plugin's documentation to see which
options are available.

Let's have a look at how you can configure `DokkaBase` plugin, which is responsible for generating [HTML](html.md) 
documentation, by adding a custom image to assets (`customAssets` option), by adding custom style sheets 
(`customStyleSheets` option) and by modifying the footer message (`footerMessage` option):

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

Gradle's Kotlin DSL allows for type-safe plugin configuration. This is achievable by adding plugin's artifact to classpath
dependencies in `buildscript` block, and then importing plugin and configuration classes:

```kotlin
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBaseConfiguration

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:%dokkaVersion%")
    }
}

tasks.withType<DokkaTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        customAssets = listOf(file("my-image.png"))
        customStyleSheets = listOf(file("my-styles.css"))
        footerMessage = "(c) 2022 MyOrg"
    }
}
```

Alternatively, plugins can be configured via JSON. This way no additional dependencies are needed.

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask

tasks.withType<DokkaTask>().configureEach {
    val dokkaBaseConfiguration = """
    {
      "customAssets": ["${file("assets/my-image.png")}"],
      "customStyleSheets": ["${file("assets/my-styles.css")}"],
      "footerMessage": "(c) 2022 MyOrg"
    }
    """
    pluginsMapConfiguration.set(
        mapOf(
            // fully qualified plugin name to json configuration
            "org.jetbrains.dokka.base.DokkaBase" to dokkaBaseConfiguration
        )
    )
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.DokkaTask

tasks.withType(DokkaTask.class) {
    String dokkaBaseConfiguration = """
    {
      "customAssets": ["${file("assets/my-image.png")}"],
      "customStyleSheets": ["${file("assets/my-styles.css")}"],
      "footerMessage": "(c) 2022 MyOrg"
    }
    """
    pluginsMapConfiguration.set(
            // fully qualified plugin name to json configuration
            ["org.jetbrains.dokka.base.DokkaBase": dokkaBaseConfiguration]
    )
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

If you are using the [CLI](cli.md) runner with [command line arguments](cli.md#running-with-command-line-arguments),
use `-pluginsConfiguration` argument that accepts JSON configuration in the form of `fullyQualifiedPluginName=json`. 

If you need to configure multiple plugins, you can pass multiple values separated by `^^`.

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     ...
     -pluginsConfiguration "org.jetbrains.dokka.base.DokkaBase={\"customAssets\": [\"my-image.png\"], \"customStyleSheets\": [\"my-styles.css\"], \"footerMessage\": \"(c) 2022 MyOrg CLI\"}"
```

If you are using [JSON configuration](cli.md#running-with-json-configuration), there exists a similar 
`pluginsConfiguration` array that accepts JSON as values.

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

| **Name**                                                                                                  | **Description**                                                                                          |
|-----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [Android documentation plugin](https://github.com/Kotlin/dokka/tree/master/plugins/android-documentation) | Improves documentation experience on Android platform                                                    |
| [Versioning plugin](versioning.md)                                                                        | Adds version selector and helps organize documentation for different version of your application/library |
| [MermaidJS HTML plugin](https://github.com/glureau/dokka-mermaid)                                         | Renders [MermaidJS](https://mermaid-js.github.io/mermaid/#/) diagrams and visualizations found in KDocs  |
| [Mathjax HTML plugin](https://github.com/Kotlin/dokka/tree/master/plugins/mathjax)                        | Pretty prints mathematics found in KDocs                                                                 |
| [Kotlin as Java plugin](https://github.com/Kotlin/dokka/tree/master/plugins/kotlin-as-java)               | Renders Kotlin signatures as seen from Java's perspective                                                |
