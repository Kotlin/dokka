[//]: # (title: Gradle)

> This guide applies to Dokka Gradle plugin (DGP) v2 mode. The previous DGP v1 mode is no longer supported.
> If you're upgrading from v1 to v2 mode, see the [Migration guide](dokka-migration.md).
>
{style="note"}

To generate documentation for a Gradle-based project, you can use the 
[Gradle plugin for Dokka](https://plugins.gradle.org/plugin/org.jetbrains.dokka).

The Dokka Gradle plugin (DGP) comes with basic autoconfiguration for your project, 
includes [Gradle tasks](#generate-documentation) for 
generating documentation, and provides [configuration options](#configuration-options) to 
customize the output.

You can play around with Dokka and explore how to configure it for various projects in our
[Gradle example projects](https://github.com/Kotlin/dokka/tree/2.0.0/examples/gradle-v2).

## Apply Dokka

The recommended way of applying the Gradle plugin for Dokka is with the 
[plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block).
Add it in the `plugins {}` block of your project’s `build.gradle.kts` file:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

</tab>
</tabs>

When documenting multi-project builds, you don't need to apply the plugin explicitly to every subproject you want to document.
Instead, 
Dokka expects you to share configuration across subprojects using convention plugins or manual configuration per subproject.
For more information, see 
how to configure [single-project](#single-project-configuration) and [multi-project](#multi-project-configuration) builds.

> * Under the hood, 
> Dokka uses the [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin) 
> to automatically configure [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) 
> for which documentation is generated. Make sure to apply the Kotlin Gradle Plugin or
> [configure source sets](#source-set-configuration) manually.
>
> * If you are using Dokka in a
> [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins),
> add the [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin) 
> as a dependency to ensure it works properly.
>
{style="tip"}

If you are not able to use the plugins DSL, you can apply the plugin using
[the legacy method](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application).

## Enable build cache and configuration cache

DGP supports Gradle build cache and configuration cache, improving build performance.

* To enable build cache, follow instructions in the [Gradle build cache documentation](https://docs.gradle.org/current/userguide/build_cache.html#sec:build_cache_enable).
* To enable configuration cache, follow instructions in the [Gradle configuration cache documentation](https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:usage:enable ).

## Generate documentation

The Dokka Gradle plugin comes with [HTML](dokka-html.md) and [Javadoc](dokka-javadoc.md) output formats 
built in.

Use the following Gradle task to generate documentation:

```shell
./gradlew :dokkaGenerate
```

The key behavior of the `dokkaGenerate` Gradle task is:

* This task generates documentation for both [single](#single-project-configuration)
  and [multi-project](#multi-project-configuration) builds.
* By default, the documentation output format is HTML. 
  You can also generate Javadoc or both HTML and Javadoc formats
  by [adding the appropriate plugins](#configure-documentation-output-format).
* The generated documentation is automatically placed in the `build/dokka/html` 
  directory for both single and multi-project builds. 
  You can [change the location (`outputDirectory`)](#general-configuration).

### Configure documentation output format

> The Javadoc output format is in [Alpha](https://kotlinlang.org/docs/components-stability.html#stability-levels-explained).
> You may find bugs and experience migration issues when using it. 
> Successful integration with tools that accept Javadoc as input is not guaranteed. 
> Use it at your own risk.
>
{style="warning"}

You can choose to generate the API documentation in HTML, Javadoc,
or both formats at the same time:

1. Place the corresponding plugin `id` in the `plugins {}` block of your project's `build.gradle.kts` file:

   ```kotlin
   plugins {
       // Generates HTML documentation
       id("org.jetbrains.dokka") version "%dokkaVersion%"

       // Generates Javadoc documentation
       id("org.jetbrains.dokka-javadoc") version "%dokkaVersion%"

       // Keeping both plugin IDs generates both formats
   }
   ```

2. Run the corresponding Gradle task.

   Here is a list of the plugin `id` and Gradle task that correspond to each format:

   |             | **HTML**                                  | **Javadoc**                                  | **Both**                          |
   |-------------|-------------------------------------------|----------------------------------------------|-----------------------------------|
   | Plugin `id` | `id("org.jetbrains.dokka")`               | `id("org.jetbrains.dokka-javadoc")`          | Use both HTML and Javadoc plugins |
   | Gradle task | `./gradlew :dokkaGeneratePublicationHtml` | `./gradlew :dokkaGeneratePublicationJavadoc` | `./gradlew :dokkaGenerate`        |

    > The `dokkaGenerate` task generates documentation in all available formats based on the applied plugins.
    > If both the HTML and Javadoc plugins are applied, 
    > you can choose to generate only HTML by running the `dokkaGeneratePublicationHtml` task,
    > or only Javadoc by running the `dokkaGeneratePublicationJavadoc` task.
    >
    {style="tip"}

###  Aggregate documentation output in multi-project builds

Dokka can aggregate documentation from multiple subprojects into a single output or publication.

You have to [apply the Dokka plugin](#apply-the-convention-plugin-to-your-subprojects) to 
all documentable subprojects before aggregating the documentation.

To aggregate documentation from multiple subprojects, add the `dependencies {}` 
block in the `build.gradle.kts` file of the root project:

```kotlin
dependencies {
    dokka(project(":childProjectA:"))
    dokka(project(":childProjectB:"))
}
```

Given a project with the following structure: 

```text
.
└── parentProject/
    ├── childProjectA/
    │   └── demo/
    │       └── ChildProjectAClass
    └── childProjectB/
        └── demo/
            └── ChildProjectBClass
```

The generated documentation is aggregated as follows:

![Screenshot for output of dokkaHtmlMultiModule task](dokkaHtmlMultiModule-example.png){width=600}

See our [multi-project example](https://github.com/Kotlin/dokka/tree/2.0.0/examples/gradle-v2/multimodule-example)
for more details.

#### Directory of aggregated documentation

When DGP aggregates subprojects, each subproject has its own subdirectory within the aggregated documentation.
DGP ensures each subproject has a unique directory by retaining the full project structure.

For example, given a project with an aggregation in `:turbo-lib` and a nested subproject `:turbo-lib:maths`, 
the generated documentation is placed under:

```text
turbo-lib/build/dokka/html/turbo-lib/maths/
```

You can revert this behavior by manually specifying the subproject directory. 
Add the following configuration to the `build.gradle.kts` file of each subproject:

```kotlin
// /turbo-lib/maths/build.gradle.kts

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // Overrides the subproject directory
    modulePath.set("maths")
}
```

## Build javadoc.jar

If you want to publish your library to a repository, you may need to provide a `javadoc.jar` file that contains 
API reference documentation of your library. 

For example, if you want to publish to [Maven Central](https://central.sonatype.org/), you 
[must](https://central.sonatype.org/publish/requirements/) supply a `javadoc.jar` alongside your project. However,
not all repositories have that rule.

The Gradle plugin for Dokka does not provide any way to do this out of the box, but it can be achieved with custom Gradle
tasks. One for generating documentation in [HTML](dokka-html.md) format and another one for [Javadoc](dokka-javadoc.md) format:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
// To generate documentation in HTML
val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// To generate documentation in Javadoc
val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
// To generate documentation in HTML
tasks.register('dokkaHtmlJar', Jar) {
    description = 'A HTML Documentation JAR containing Dokka HTML'
    from(tasks.named('dokkaGeneratePublicationHtml').flatMap { it.outputDirectory })
    archiveClassifier.set('html-doc')
}

// To generate documentation in Javadoc
tasks.register('dokkaJavadocJar', Jar) {
    description = 'A Javadoc JAR containing Dokka Javadoc'
    from(tasks.named('dokkaGeneratePublicationJavadoc').flatMap { it.outputDirectory })
    archiveClassifier.set('javadoc')
}
```

</tab>
</tabs>

> If you publish your library to Maven Central, you can use services like [javadoc.io](https://javadoc.io/) to
> host your library's API documentation for free and without any setup. It takes documentation pages straight
> from the `javadoc.jar`. It works well with the HTML format as demonstrated in
> [this example](https://javadoc.io/doc/com.trib3/server/latest/index.html).
>
{style="tip"}

## Configuration examples

Depending on the type of project that you have, the way you apply and configure Dokka differs slightly. However,
[configuration options](#configuration-options) themselves are the same, regardless of the type of your project.

For simple and flat projects with a single `build.gradle.kts` or `build.gradle` file found in the root of your project,
see [Single-project configuration](#single-project-configuration).

For a more complex build with subprojects and multiple nested `build.gradle.kts` or `build.gradle` files,
see [Multi-project configuration](#multi-project-configuration).

### Single-project configuration

Single-project builds usually have only one `build.gradle.kts` 
or `build.gradle` file in the root of the project.
They can be either single-platform or multiplatform and typically have the following structure:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

Single platform:

```text
.
├── build.gradle.kts
└── src/
    └── main/
        └── kotlin/
            └── HelloWorld.kt
```

Multiplatform:

```text
.
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   └── kotlin/
    │       └── Common.kt
    ├── jvmMain/
    │   └── kotlin/
    │       └── JvmUtils.kt
    └── nativeMain/
        └── kotlin/
            └── NativeUtils.kt
```

</tab>
<tab title="Groovy" group-key="groovy">

Single platform:

```text
.
├── build.gradle
└── src/
    └── main/
        └── kotlin/
            └── HelloWorld.kt
```

Multiplatform:

```text
.
├── build.gradle
└── src/
    ├── commonMain/
    │   └── kotlin/
    │       └── Common.kt
    ├── jvmMain/
    │   └── kotlin/
    │       └── JvmUtils.kt
    └── nativeMain/
        └── kotlin/
            └── NativeUtils.kt
```

</tab>
</tabs>

Apply the Dokka Gradle plugin in your root `build.gradle.kts` file and configure it using the top-level `dokka {}` DSL:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dokka {
    dokkaPublications.html {
        moduleName.set("MyProject")
        outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
        includes.from("README.md")
   }

    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(URI("https://github.com/your-repo"))
            remoteLineSuffix.set("#L")
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

Inside `./build.gradle`:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}

dokka {
    dokkaPublications {
        html {
            moduleName.set("MyProject")
            outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
            includes.from("README.md")
        }
    }

    dokkaSourceSets {
        named("main") {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(new URI("https://github.com/your-repo"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
```

</tab>
</tabs>

This configuration applies Dokka to your project, 
sets up the documentation output directory, and defines the main source set.
You can extend it further by adding custom assets, visibility filters, 
or plugin configurations within the same `dokka {}` block.
For more information, see [Configuration options](#configuration-options).

### Multi-project configuration

[Multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html) 
usually contain several 
nested `build.gradle.kts` files and have a structure similar to the following:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```text
.
├── build.gradle.kts
├── settings.gradle.kts
├── subproject-A/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           └── kotlin/
│               └── HelloFromA.kt
└── subproject-B/
    ├── build.gradle.kts
    └── src/
        └── main/
            └── kotlin/
                └── HelloFromB.kt
```

</tab>
<tab title="Groovy" group-key="groovy">

```text
.
├── build.gradle
├── settings.gradle
├── subproject-A/
│   ├── build.gradle
│   └── src/
│       └── main/
│           └── kotlin/
│               └── HelloFromA.kt
└── subproject-B/
    ├── build.gradle
    └── src/
        └── main/
            └── kotlin/
                └── HelloFromB.kt
```

</tab>
</tabs>

Single and multi-project documentation share the same 
[configuration model using the top-level `dokka {}` DSL](#single-project-configuration).
In multi-project builds, 
you configure Dokka in the root project and can optionally share settings across subprojects.

To share Dokka configuration across subprojects, you can use either:

* [Direct configuration in multi-project builds requiring convention plugins](#direct-configuration-in-multi-project-builds-requiring-convention-plugins)
* [Convention plugins](#multi-project-builds-with-convention-plugins)

After sharing Dokka configuration, you can aggregate the documentation from multiple subprojects into a single output. 
For more information, see
[Aggregate documentation output in multi-project-builds](#aggregate-documentation-output-in-multi-project-builds).

> For a multi-project example, see the [Dokka GitHub repository](https://github.com/Kotlin/dokka/tree/master/examples/gradle-v2/multimodule-example).
>
{style="tip"}

#### Direct configuration in multi-project builds requiring convention plugins

If your project doesn't use convention plugins, you can share Dokka configurations by directly configuring each subproject.
This involves manually setting up the shared configuration in each subproject's `build.gradle.kts` file. 
While this approach is less centralized,
it avoids the need for additional setups like convention plugins.

Follow the next steps to configure your multi-project builds without convention plugins.

##### Set up the buildSrc directory

1. In your project root, create a `buildSrc` directory containing two files:

    * `settings.gradle.kts`
    * `build.gradle.kts`

2. In the `buildSrc/settings.gradle.kts` file, add the following snippet:

   ```kotlin
   rootProject.name = "buildSrc"
   ```

3. In the `buildSrc/build.gradle.kts` file, add the following snippet:

    ```kotlin
    plugins {
        `kotlin-dsl`
    }
    
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
    dependencies {
        implementation("org.jetbrains.dokka:dokka-gradle-plugin:%dokkaVersion%")
    }   
    ```

##### Set up the Dokka convention plugin

After setting up the `buildSrc` directory, set up the Dokka convention plugin:

1. Create a `buildSrc/src/main/kotlin/dokka-convention.gradle.kts` file to host the [convention plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:convention_plugins).
2. In the `dokka-convention.gradle.kts` file, add the following snippet:

    ```kotlin
    plugins {
        id("org.jetbrains.dokka") 
    }

    dokka {
        // The shared configuration goes here
    }
    ```

   You need to add the shared Dokka [configuration](#configuration-options) common to all subprojects within the `dokka {}` 
   block.
   Also, you don't need to specify a Dokka version. 
   The version is already set in the `buildSrc/build.gradle.kts` file.

##### Apply the convention plugin to your subprojects

Apply the Dokka convention plugin across your subprojects by adding it to each subproject's `build.gradle.kts` 
file:

```kotlin
plugins {
    id("dokka-convention")
}
```

#### Multi-project builds with convention plugins

If you already have convention plugins, 
create a dedicated Dokka convention plugin following [Gradle's documentation](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:convention_plugins).

Then, follow the steps to [set up the Dokka convention plugin](#set-up-the-dokka-convention-plugin) and
[apply it across your subprojects](#apply-the-convention-plugin-to-your-subprojects).

#### Parent project configuration

In multi-project builds, you can configure settings that apply to the entire documentation in the root project.
This can include defining the output format, output directory, documentation subproject name, 
aggregating documentation from all
subprojects, and other [configuration options](#configuration-options):

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dokka {
    // Sets properties for the whole project
    dokkaPublications.html {
        moduleName.set("My Project")
        outputDirectory.set(layout.buildDirectory.dir("docs/html"))
        includes.from("README.md")
    }

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)    
    }
}

// Aggregates subproject documentation
dependencies {
    dokka(project(":childProjectA"))
    dokka(project(":childProjectB"))
}
```

Additionally, each subproject can have its own `dokka {}` block if it needs custom configuration.
In the following example, the subproject applies the Dokka plugin, sets a custom subproject name, 
and includes additional documentation from its `README.md` file:

```kotlin
// subproject/build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaPublications.html {
        moduleName.set("Child Project A")
        includes.from("README.md")
    }
}
```

## Configuration options

Dokka has many configuration options to tailor your and your reader's experience. 

Below are some examples and detailed descriptions for each configuration section. You can also find an example
with [all configuration options](#complete-configuration) applied.

See [Configuration examples](#configuration-examples) for more details on where to apply configuration blocks and how.

### General configuration

Here is an example of the general Dokka Gradle plugin configuration. Use the top-level `dokka {}` DSL configuration.

In DGP, `dokkaPublications` is the central way to declare Dokka publication configurations. 
The default publications
are [`html`](dokka-html.md), [`javadoc`](dokka-javadoc.md), and [`gfm`](https://github.com/Kotlin/dokka/blob/8e5c63d035ef44a269b8c43430f43f5c8eebfb63/dokka-subprojects/plugin-gfm/README.md).

The syntax of `build.gradle.kts` files differs from regular `.kt` 
files (such as those used for custom Gradle plugins) because Gradle's Kotlin DSL uses type-safe accessors:

<tabs group="build-script">
<tab title="Gradle configuration" group-key="Gradle">

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dokka {
    dokkaPublications.html {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        failOnWarning.set(false)
        suppressInheritedMembers.set(false)
        offlineMode.set(false)
        suppressObviousFunctions.set(true)
        includes.from(project.files(), "packages.md", "extra.md")

        // Output directory for additional files
        // Use this block when you want to change the output directory and include extra files
        outputDirectory.set(rootDir.resolve("docs/api/0.x"))
        includes.from(project.layout.projectDirectory.file("README.md"))
    }
}
```

</tab>
<tab title="Kotlin custom plugin" group-key="kotlin">

```kotlin
// CustomPlugin.kt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

abstract class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.dokka")

        project.extensions.configure(DokkaExtension::class.java) { dokka ->
            
            dokka.moduleName.set(project.name)
            dokka.moduleVersion.set(project.version.toString())

            dokka.dokkaPublications.named("html") { publication ->
                // Standard output directory for HTML documentation
                publication.outputDirectory.set(project.layout.buildDirectory.dir("dokka/html"))
                publication.failOnWarning.set(true)
                publication.suppressInheritedMembers.set(true)
                publication.offlineMode.set(false)
                publication.suppressObviousFunctions.set(true)
                publication.includes.from(project.files(), "packages.md", "extra.md")

                // Output directory for additional files
                // Use this block when you want to change the output directory and include extra files
                html.outputDirectory.set(project.rootDir.resolve("docs/api/0.x"))
                html.includes.from(project.layout.projectDirectory.file("README.md"))
            }
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}

dokka {
    dokkaPublications {
        html {
            // Sets general module information
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())

            // Standard output directory for HTML documentation
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

            // Core Dokka options
            failOnWarning.set(false)
            suppressInheritedMembers.set(false)
            offlineMode.set(false)
            suppressObviousFunctions.set(true)
            includes.from(files("packages.md", "extra.md"))

            // Output directory for additional files
            // Use this block when you want to change the output directory and include extra files
            outputDirectory.set(file("$rootDir/docs/api/0.x"))
            includes.from(layout.projectDirectory.file("README.md"))
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="moduleName">
        <p>
           The display name to refer to the project’s documentation. It appears in the table of contents, navigation, 
           headers, and log messages. In multi-project builds, each subproject <code>moduleName</code> is 
           used as its section title in aggregated documentation.
        </p>
        <p>Default: Gradle project name</p>
    </def>
    <def title="moduleVersion">
        <p>
            The subproject version displayed in the generated documentation. 
            In single-project builds, it is used as the project version.
            In multi-project builds, each subproject <code>moduleVersion</code> 
            is used when aggregating documentation. 
        </p>
        <p>Default: Gradle project version</p>
    </def>
    <def title="outputDirectory">
        <p>The directory where the generated documentation is stored.</p>
        <p>This setting applies to all documentation formats (HTML, Javadoc, etc.) generated by the <code>dokkaGenerate</code> task.</p>
        <p>Default: <code>build/dokka/html</code></p>
        <p><b>Output directory for additional files</b></p>
        <p>You can specify the output directory and include additional files for both single and multi-project builds.
           For multi-project builds,
           set the output directory and include additional files (such as <code>README.md</code>) 
           in the configuration of the root project.
        </p>
    </def>
    <def title="failOnWarning">
        <p>
            Determines whether Dokka should fail the build when a warning occurs during documentation generation.
            The process waits until all errors and warnings have been emitted first.
        </p>
        <p>This setting works well with <code>reportUndocumented</code>.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="suppressInheritedMembers">
        <p>Whether to suppress inherited members that aren't explicitly overridden in a given class.</p>
        <p>
            Note: This can suppress functions such as <code>equals</code> / <code>hashCode</code> / <code>toString</code>, 
            but cannot suppress synthetic functions such as <code>dataClass.componentN</code> and 
            <code>dataClass.copy</code>. Use <code>suppressObviousFunctions</code>
            for that.
        </p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="offlineMode">
        <p>Whether to resolve remote files/links over your network.</p>
        <p>
            This includes package-lists used for generating external documentation links. 
            For example, to make classes from the standard library clickable. 
        </p>
        <p>
            Setting this to <code>true</code> can significantly speed up build times in certain cases,
            but can also worsen documentation quality and user experience. For example, by
            not resolving class/member links from your dependencies, including the standard library.
        </p>
        <p>Note: You can cache fetched files locally and provide them to Dokka as local paths. See 
           the <code><a href="#external-documentation-links-configuration">externalDocumentationLinks</a></code> section.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="suppressObviousFunctions">
        <p>Whether to suppress obvious functions.</p>
        <p>
            A function is considered to be obvious if it is:</p>
            <list>
                <li>
                    Inherited from <code>kotlin.Any</code>, <code>Kotlin.Enum</code>, <code>java.lang.Object</code> or
                    <code>java.lang.Enum</code>, such as <code>equals</code>, <code>hashCode</code>, <code>toString</code>.
                </li>
                <li>
                    Synthetic (generated by the compiler) and does not have any documentation, such as
                    <code>dataClass.componentN</code> or <code>dataClass.copy</code>.
                </li>
            </list>
        <p>Default: <code>true</code></p>
    </def>
     <def title="includes">
        <p>
            A list of Markdown files that contain
            <a href="dokka-module-and-package-docs.md">subproject and package documentation</a>.
        </p>
        <p>The contents of the specified files are parsed and embedded into documentation as subproject and package descriptions.</p>
        <p>
            See <a href="https://github.com/Kotlin/dokka/blob/master/examples/gradle-v2/basic-gradle-example/build.gradle.kts">Dokka gradle example</a>
            for an example of what it looks like and how to use it.
        </p>
    </def>
</deflist>

### Source set configuration

Dokka allows configuring some options for 
[Kotlin source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets):

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    // ..
    // General configuration section
    // ..

    // Source sets configuration
    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets{named("native")}
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")
           
            sourceLink {
                // Source link section
            }
            perPackageOption {
                // Package options section
            }
            externalDocumentationLinks {
                // External documentation links section
            }
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    // ..
    // General configuration section
    // ..

    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets { named("native") }
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set([VisibilityModifier.Public] as Set) // OR documentedVisibilities(VisibilityModifier.Public)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(files(), file("libs/dependency.jar"))
            samples.from(files(), "samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                // Source link section
            }
            perPackageOption {
                // Package options section
            }
            externalDocumentationLinks {
                // External documentation links section
            }
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="suppress">
        <p>Whether this source set should be skipped when generating documentation.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="displayName">
        <p>The display name used to refer to this source set.</p>
        <p>
            The name is used both externally (for example, as source set name visible to documentation readers) and 
            internally (for example, for logging messages of <code>reportUndocumented</code>).
        </p>
        <p>By default, the value is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="documentedVisibilities">
        <p>Defines which visibility modifiers Dokka should include in the generated documentation.</p>
        <p>
            Use them if you want to document <code>Protected</code>/<code>Internal</code>/<code>Private</code> declarations,
            as well as if you want to exclude <code>Public</code> declarations and only document internal API.
        </p>
        <p>
            Additionally, you can use Dokka's 
            <a href="https://github.com/Kotlin/dokka/blob/v2.0.0/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/HasConfigurableVisibilityModifiers.kt#L14-L16"><code>documentedVisibilities()</code> function</a> 
            to add documented visibilities.
        </p>
        <p>This can be configured on a per-package basis.</p>
        <p>Default: <code>VisibilityModifier.Public</code></p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code> and other filters.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured on a per-package basis.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="skipEmptyPackages">
        <p>
            Whether to skip packages that contain no visible declarations after
            various filters have been applied.
        </p>
        <p>
            For example, if <code>skipDeprecated</code> is set to <code>true</code> and your package contains only
            deprecated declarations, it is considered to be empty.
        </p>
        <p>Default: <code>true</code></p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be configured on a per-package basis.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="suppressGeneratedFiles">
        <p>Whether to document/analyze generated files.</p>
        <p>
            Generated files are expected to be present under the <code>{project}/{buildDir}/generated</code> directory.
        </p>
        <p>
            If set to <code>true</code>, it effectively adds all files from that directory to the
            <code>suppressedFiles</code> option, so you can configure it manually.
        </p>
        <p>Default: <code>true</code></p>
    </def>
    <def title="jdkVersion">
        <p>The JDK version to use when generating external documentation links for Java types.</p>
        <p>
            For example, if you use <code>java.util.UUID</code> in some public declaration signature,
            and this option is set to <code>8</code>, Dokka generates an external documentation link
            to <a href="https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html">JDK 8 Javadocs</a> for it.
        </p>
        <p>Default: JDK 8</p>
    </def>
    <def title="languageVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin language version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
        <p>By default, the latest language version available to Dokka's embedded compiler is used.</p>
    </def>
    <def title="apiVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin API version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
        <p>By default, it is deduced from <code>languageVersion</code>.</p>
    </def>
    <def title="sourceRoots">
        <p>
            The source code roots to be analyzed and documented.
            Acceptable inputs are directories and individual <code>.kt</code> / <code>.java</code> files.
        </p>
        <p>By default, source roots are deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="classpath">
        <p>The classpath for analysis and interactive samples.</p>
        <p>This is useful if some types that come from dependencies are not resolved/picked up automatically.</p>
        <p>This option accepts both <code>.jar</code> and <code>.klib</code> files.</p>
        <p>By default, the classpath is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="samples">
        <p>
            A list of directories or files that contain sample functions which are referenced via the
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> KDoc tag.
        </p>
    </def>
</deflist>

### Source link configuration

Configure source links 
to allow navigation from the generated documentation to the corresponding source code in a remote repository.
Use the `dokkaSourceSets.main{}` block for this configuration.

The `sourceLinks` configuration block allows you to add a `source` link to each signature
that leads to the `remoteUrl` with a specific line number. 
The line number is configurable by setting `remoteLineSuffix`.

This helps readers to find the source code for each declaration.

For an example, see the documentation for the
[`count()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/count.html)
function in `kotlinx.coroutines`.

The syntax of `build.gradle.kts` files differs from regular `.kt` 
files (such as those used for custom Gradle plugins) because Gradle's Kotlin DSL uses type-safe accessors:

<tabs group="dokka-configuration">
<tab title="Gradle configuration" group-key="gradle">

```kotlin
// build.gradle.kts

dokka {
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/your-repo")
            remoteLineSuffix.set("#L")
        }
    }
}
```

</tab>
<tab title="Kotlin custom plugin" group-key="kotlin">

```kotlin
// CustomPlugin.kt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

abstract class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.dokka")
        project.extensions.configure(DokkaExtension::class.java) { dokka ->
            dokka.dokkaSourceSets.named("main") { dss ->
                dss.includes.from("README.md")
                dss.sourceLink {
                    it.localDirectory.set(project.file("src/main/kotlin"))
                    it.remoteUrl("https://example.com/src")
                    it.remoteLineSuffix.set("#L")
                }
            }
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
dokka {
    dokkaSourceSets {
        main {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(new URI("https://github.com/your-repo"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="localDirectory">
        <p>
            The path to the local source directory. The path must be relative to the root of 
            the current project.
        </p>
    </def>
    <def title="remoteUrl">
        <p>
            The URL of the source code hosting service that can be accessed by documentation readers,
            like GitHub, GitLab, Bitbucket, etc. This URL is used to generate
            source code links of declarations.
        </p>
    </def>
    <def title="remoteLineSuffix">
        <p>
            The suffix used to append the source code line number to the URL. This helps readers navigate
            not only to the file, but to the specific line number of the declaration.
        </p>
        <p>
            The number itself is appended to the specified suffix. For example,
            if this option is set to <code>#L</code> and the line number is 10, the resulting URL suffix
            is <code>#L10</code>.
        </p>
        <p>
            Suffixes used by popular services:</p>
            <list>
                <li>GitHub: <code>#L</code></li>
                <li>GitLab: <code>#L</code></li>
                <li>Bitbucket: <code>#lines-</code></li>
            </list>
        <p>Default: <code>#L</code></p>
    </def>
</deflist>

Since the source link configuration has [changed](https://docs.gradle.org/current/userguide/upgrading_version_8.html#deprecated_invalid_url_decoding),
use the `URI` class to specify the remote URL:

```kotlin
remoteUrl.set(URI("https://github.com/your-repo"))

// or

remoteUrl("https://github.com/your-repo")
```

Additionally, 
you can use two [utility functions](https://github.com/Kotlin/dokka/blob/220922378e6c68eb148fda2ec80528a1b81478c9/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/DokkaSourceLinkSpec.kt#L82-L96)
for setting the URL:

```kotlin
fun remoteUrl(@Language("http-url-reference") value: String): Unit =
    remoteUrl.set(URI(value))

// and

fun remoteUrl(value: Provider<String>): Unit =
    remoteUrl.set(value.map(::URI))
```

### Package options

The `perPackageOption` configuration block allows setting some options for specific packages matched by `matchingRegex`:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    dokkaPublications.html {
        dokkaSourceSets.configureEach {
            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)
            }
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    dokkaPublications {
        html {
            dokkaSourceSets.configureEach {
                perPackageOption {
                    matchingRegex.set(".*api.*")
                    suppress.set(false)
                    skipDeprecated.set(false)
                    reportUndocumented.set(false)
                    documentedVisibilities.set([VisibilityModifier.Public] as Set)
                }
            }
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="matchingRegex">
        <p>The regular expression that is used to match the package.</p>
        <p>Default: <code>.*</code></p>
    </def>
    <def title="suppress">
        <p>Whether this package should be skipped when generating documentation.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be configured on the source set level.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code> and other filters.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured on the source set level.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="documentedVisibilities">
        <p>Defines which visibility modifiers Dokka should include in the generated documentation.</p>
        <p>
            Use them if you want to document <code>Protected</code>/<code>Internal</code>/<code>Private</code> 
            declarations within this package,
            as well as if you want to exclude <code>Public</code> declarations and only document internal API.
        </p>
        <p>
            Additionally, you can use Dokka's 
            <a href="https://github.com/Kotlin/dokka/blob/v2.0.0/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/HasConfigurableVisibilityModifiers.kt#L14-L16"><code>documentedVisibilities()</code> function</a> 
            to add documented visibilities.
        </p>
        <p>This can be configured on the source set level.</p>
        <p>Default: <code>VisibilityModifier.Public</code></p>
    </def>
</deflist>

### External documentation links configuration

The `externalDocumentationLinks {}` 
block allows the creation of links that lead to the externally hosted documentation of 
your dependencies.

For example, if you are using types from `kotlinx.serialization`, by default they are unclickable in your
documentation, as if they are unresolved. However, since the API reference documentation for `kotlinx.serialization` 
is built by Dokka and is [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/), you can 
configure external documentation links for it. Thus allowing Dokka to generate links for types from the library, making 
them resolve successfully and clickable.

By default, external documentation links for Kotlin standard library, JDK, Android SDK, and AndroidX are configured.

Register external documentation links using the `register()` method to define each link.
The `externalDocumentationLinks` API uses this method aligning with Gradle DSL conventions:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("example-docs") {
            url("https://example.com/docs/")
            packageListUrl("https://example.com/docs/package-list")
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("example-docs") {
            url.set(new URI("https://example.com/docs/"))
            packageListUrl.set(new URI("https://example.com/docs/package-list"))
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="url">
        <p>The root URL of documentation to link to. It <b>must</b> contain a trailing slash.</p>
        <p>
            Dokka does its best to automatically find <code>package-list</code> for the given URL, 
            and link declarations together.
        </p>
        <p>
            If automatic resolution fails or if you want to use locally cached files instead, 
            consider setting the <code>packageListUrl</code> option.
        </p>
    </def>
    <def title="packageListUrl">
        <p>
            The exact location of a <code>package-list</code>. This is an alternative to relying on Dokka
            automatically resolving it.
        </p>
        <p>
            Package lists contain information about the documentation and the project itself, 
            such as subproject and package names.
        </p>
        <p>This can also be a locally cached file to avoid network calls.</p>
    </def>
</deflist>

### Complete configuration

Below you can see all possible configuration options applied at the same time:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dokka {
    dokkaPublications.html {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        failOnWarning.set(false)
        suppressInheritedMembers.set(false)
        offlineMode.set(false)
        suppressObviousFunctions.set(true)
        includes.from(project.files(), "packages.md", "extra.md")
   }

    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets{named("native")}
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://example.com/src")
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks {
                url = URL("https://example.com/docs/")
                packageListUrl = File("/path/to/package-list").toURI().toURL()
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(
                    setOf(
                        VisibilityModifier.Public,
                        VisibilityModifier.Private,
                        VisibilityModifier.Protected,
                        VisibilityModifier.Internal,
                        VisibilityModifier.Package
                    )
                )
            }
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}

dokka {
    dokkaPublications {
        html {
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
            failOnWarning.set(false)
            suppressInheritedMembers.set(false)
            offlineMode.set(false)
            suppressObviousFunctions.set(true)
            includes.from(files(), "packages.md", "extra.md")
        }
    }

    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets { named("native") }
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set([VisibilityModifier.Public] as Set)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(files(), file("libs/dependency.jar"))
            samples.from(files(), "samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(new URI("https://example.com/src"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks {
                url.set(new URI("https://example.com/docs/"))
                packageListUrl.set(new File("/path/to/package-list").toURI().toURL())
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set([
                        VisibilityModifier.Public,
                        VisibilityModifier.Private,
                        VisibilityModifier.Protected,
                        VisibilityModifier.Internal,
                        VisibilityModifier.Package
                ] as Set)
            }
        }
    }
}
```

</tab>
</tabs>
