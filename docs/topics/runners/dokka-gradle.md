[//]: # (title: Gradle)

> This guide applies to Dokka Gradle plugin (DGP) v2 mode. The previous DGP v1 mode is no longer supported.
> If you're upgrading from v1 to v2 mode, see the [Migration guide](dokka-migration.md).
>
{style="note"}

To generate documentation for a Gradle-based project, you can use the 
[Gradle plugin for Dokka](https://plugins.gradle.org/plugin/org.jetbrains.dokka).

The Dokka Gradle plugin (DGP) comes with basic autoconfiguration for your project, 
includes [Gradle tasks](#generate-documentation) for 
generating documentation, and provides [configuration options](dokka-gradle-configuration-options.md) to 
customize the output.

You can play around with Dokka and explore how to configure it for various projects in our
[Gradle example projects](https://github.com/Kotlin/dokka/tree/2.0.0/examples/gradle-v2).

## Supported versions

Ensure that your project meets the minimum version requirements:

| **Tool**                                                                          | **Version**   |
|-----------------------------------------------------------------------------------|---------------|
| [Gradle](https://docs.gradle.org/current/userguide/upgrading_version_8.html)      | 7.6 or higher |
| [Android Gradle plugin](https://developer.android.com/build/agp-upgrade-assistant) | 7.0 or higher |
| [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html) | 1.9 or higher |

## Apply Dokka

The recommended way of applying the Gradle plugin for Dokka is with the 
[plugins block](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block).
Add it in the `plugins {}` block of your project’s `build.gradle.kts` file:

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

</tab>
</tabs>

When documenting multi-project builds, you need to apply the plugin explicitly to every subproject you want to document.
You can configure Dokka directly in each subproject or share Dokka configuration across subprojects using a convention plugin.
For more information, see 
how to configure [single-project](#single-project-configuration) and [multi-project](#multi-project-configuration) builds.

> * Under the hood, 
> Dokka uses the [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin) 
> to automatically configure [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) 
> for which documentation is generated. Make sure to apply the Kotlin Gradle Plugin or
> [configure source sets](dokka-gradle-configuration-options.md#source-set-configuration) manually.
>
> * If you are using Dokka in a
> [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins),
> add the [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin) 
> as a dependency to ensure it works properly.
>
{style="tip"}

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
  You can [change the location (`outputDirectory`)](dokka-gradle-configuration-options.md#general-configuration).

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

    > * The `dokkaGenerate` task generates documentation in all available formats based on the applied plugins.
    > If both the HTML and Javadoc plugins are applied, 
    > you can choose to generate only HTML by running the `dokkaGeneratePublicationHtml` task,
    > or only Javadoc by running the `dokkaGeneratePublicationJavadoc` task.
    > 
    {style="tip"}

If you're using IntelliJ IDEA, you may see the `dokkaGenerateHtml` Gradle task.
This task is simply an alias of `dokkaGeneratePublicationHtml`. Both tasks perform exactly the same operation.

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
    │       └── ChildProjectAClass.kt
    └── childProjectB/
        └── demo/
            └── ChildProjectBClass.kt
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

This configuration changes the generated documentation for the `:turbo-lib:maths` module to 
be generated into `turbo-lib/build/dokka/html/maths/`.

## Build javadoc.jar

If you want to publish your library to a repository, you may need to provide a `javadoc.jar` file that contains 
API reference documentation of your library. 

For example, if you want to publish to [Maven Central](https://central.sonatype.org/), you 
[must](https://central.sonatype.org/publish/requirements/) supply a `javadoc.jar` alongside your project. However,
not all repositories have that rule.

The Gradle plugin for Dokka does not provide any way to do this out of the box, but it can be achieved with custom Gradle
tasks. One for generating documentation in [HTML](dokka-html.md) format and another one for [Javadoc](dokka-javadoc.md) format:

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

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
<tab title="Gradle Groovy DSL" group-key="groovy">

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
[configuration options](dokka-gradle-configuration-options.md) themselves are the same, regardless of the type of your project.

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
<tab title="Gradle Kotlin DSL" group-key="kotlin">

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
<tab title="Gradle Groovy DSL" group-key="groovy">

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
For more information, see [Configuration options](dokka-gradle-configuration-options.md).

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

There are two ways of configuring Dokka in multi-project builds:

* **[Shared configuration via a convention plugin](#shared-configuration-via-a-convention-plugin) (preferred)**: defining a convention plugin and applying it to all subprojects.
  This centralizes your Dokka settings.

* **[Manual configuration](#manual-configuration)**: applying the Dokka plugin and repeating the same `dokka {}` block
  in each subproject. 
  You don't need convention plugins.

After configuring your subprojects, you can aggregate the documentation from multiple subprojects into a single output. 
For more information, see
[Aggregate documentation output in multi-project-builds](#aggregate-documentation-output-in-multi-project-builds).

> For a multi-project example, see the [Dokka GitHub repository](https://github.com/Kotlin/dokka/tree/master/examples/gradle-v2/multimodule-example).
>
{style="tip"}

#### Shared configuration via a convention plugin

Follow the next steps to set up a convention plugin and apply it to your subprojects.

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

   You need to add the shared Dokka [configuration](dokka-gradle-configuration-options.md) common to all subprojects within the `dokka {}` 
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

#### Manual configuration

If your project doesn't use convention plugins, you can reuse the same Dokka configuration pattern by manually 
copying the same `dokka {} block` into each subproject:

1. Apply the Dokka plugin in every subproject's `build.gradle.kts` file:

   ```kotlin
   plugins {
       id("org.jetbrains.dokka") version "%dokkaVersion%"
   }
   ```

2. Declare the shared configuration in each subproject's `dokka {}` block. Because there's no convention plugin centralizing 
   the configuration, you duplicate any configuration you want across subprojects. For more information,
   see [configuration options](dokka-gradle-configuration-options.md).

#### Parent project configuration

In multi-project builds, you can configure settings that apply to the entire documentation in the root project.
This can include defining the output format, output directory, documentation subproject name, 
aggregating documentation from all
subprojects, and other [configuration options](dokka-gradle-configuration-options.md):

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