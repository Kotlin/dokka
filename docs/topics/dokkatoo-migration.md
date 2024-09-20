[//]: # (title: Migrate to Dokka Gradle plugin v2)

> The Dokka Gradle plugin v2 is an [experimental](https://kotlinlang.org/docs/components-stability.html#stability-levels-explained) feature. 
> It may be changed at any time. We appreciate your feedback on [GitHub](https://github.com/Kotlin/dokka/issues).
>
{type="warning"}

The Dokka Gradle plugin (DGP) is a tool to generate comprehensive API documentation for Kotlin projects.

DGP seamlessly processes both Kotlin's KDoc comments and Java's Javadoc comments to extract information and create 
structured documentation in [HTML or Javadoc](#select-documentation-output-format) format.

From Dokka 2.0.0, you can try the Dokka Gradle plugin v2, the new version of DGP.

DGP v2 introduces significant improvements to DGP, aligning more closely with Gradle best practices. Key improvements are:

* Adoption of Gradle types, leading to better performance.
* Use of intuitive top-level DSL configuration instead of low-level task-based setup, simplifying the build scripts and their readability.
* More declarative approach to documentation aggregation, making multi-project documentation easier to manage.
* Plugin configuration is now typesafe, improving the reliability and maintainability of your build scripts.

## Before you start

Before starting the migration, complete the following steps.

### Verify supported versions

Ensure your project meets the minimum version requirements:

| **Tool**              | **Version**   |
|-----------------------|---------------|
| [Gradle](https://docs.gradle.org/current/userguide/upgrading_version_8.html)                | 7.6 or higher |
| [Android Gradle plugin](https://developer.android.com/build/agp-upgrade-assistant) | 7.0 or higher |
| [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-configure-project.html)  | 1.9 or higher |

### Enable the new Dokka Gradle plugin

1. Update the Dokka version to 2.0.0 in the `plugins {}` block of your project’s `build.gradle.kts` file:

   ```kotlin
   plugins {
       kotlin("jvm") version "1.9.25"
       id("org.jetbrains.dokka") version "2.0.0"
   }
   ```

   > The default output format for DGP v2 is HTML. For more information about getting Javadoc output, see 
   > [Select documentation output format](#select-documentation-output-format).
   >
   {type="tip"}

   Alternatively, you can use [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog) to enable the Dokka Gradle plugin v2.

2. In the project's `gradle.properties` file, set the following opt-in flag with helpers to activate the new plugin version:

   ```kotlin
   org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers
   ```

   If your project does not have a `gradle.properties` file, create one in the root directory of your project.

3. Sync your project with Gradle to ensure the new plugin is properly applied:

   * If you use IntelliJ IDEA, click the **Reload** button from the Gradle tool window.
   * If you use Android Studio, select **File** | **Sync Project with Gradle Files**.

## Migrate your project

After updating the Dokka Gradle plugin to v2, follow the migration steps applicable to your project.

### Adjust configuration options

DGP v2 introduces some changes in the Gradle configuration options. In the `build.gradle.kts` file, adjust the configuration 
options according to your project setup: 

* **New top-level DSL configuration:** Replace the old configuration syntax with the 
  new top-level `dokka {}` DSL configuration. For example:

  Previous configuration:

  ```kotlin
  tasks.withType<DokkaTask>().configureEach {
      dokkaSourceSets {
          named("main") {
              moduleName.set("Project Name")
              includes.from("README.md")
              sourceLink {
                  localDirectory.set(file("src/main/kotlin"))
                  remoteUrl.set(URL("https://example.com/src"))
                  remoteLineSuffix.set("#L")
              }
          }
      }
  }

  tasks.dokkaHtml {
      pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
          customStyleSheets.set(listOf("styles.css"))
          customAssets.set(listOf("logo.png"))
          footerMessage.set("(c) Your Company")
      }
  }
  ```
 
  New configuration:

  ```kotlin
  dokka {
      moduleName = "Project Name"
      dokkaSourceSets.main {
          includes.from("README.md")
          sourceLink {
              localDirectory = file("src/main/kotlin")
              remoteUrl = URL("https://example.com/src")
              remoteLineSuffix = "#L"
          }
      }
      pluginsConfiguration.html {
          customStyleSheets.from("styles.css")
          customAssets.from("logo.png")
          footerMessage = "(c) Your Company"
      }
  }
  ```

* **Visibility settings:**  The `documentedVisibilities` property has changed from `Visibility.PUBLIC` to `VisibilityModifier.Public`.

  Previous configuration:

  ```kotlin
  import org.jetbrains.dokka.DokkaConfiguration.Visibility
  ...
  documentedVisibilities.set(
      setOf(Visibility.PUBLIC)
  )  
  ```

  New configuration:

  ```kotlin
  import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
  ...
  documentedVisibilities.set(
      setOf(VisibilityModifier.Public)
  )
  ```

* **External documentation link:** The source link configuration has [changed](https://docs.gradle.org/current/userguide/upgrading_version_8.html#deprecated_invalid_url_decoding). 
    The remote URL is now specified using `URI` instead of `URL`.

  Previous configuration:
  
    ```kotlin
    remoteUrl.set(URL("https://github.com/your-repo"))   
    ```
  
  New configuration:
  
    ```kotlin
    remoteUrl.set(URI("https://github.com/your-repo"))  
    ```

* **Custom assets:** The [`customAssets`](dokka-html.md#customize-assets) property now uses collections of files (`FileCollection`) instead of lists (`var List<File>`).

  Previous configuration:

    ```kotlin
    customAssets = listOf(file("example.png"), file("example2.png"))   
    ```

  New configuration:

    ```kotlin
    customAssets.from("example.png", "example2.png")
    ```

### Share Dokka configuration across modules

DPG v2 moves away from using `subprojects {}` or `allprojects {}` to share configuration across modules. In future Gradle versions, 
using these approaches will lead to errors. For more information, see [Gradle's documentation about isolated projects](https://docs.gradle.org/current/userguide/isolated_projects.html).

Follow the steps below to properly share Dokka configuration in multi-module projects with DPG v2.

> For a multi-module project example, see the [Dokka GitHub repository](https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example).
>
{type="tip"}

#### Multi-module projects without convention plugins

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
        implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    }   
    ```
   
4. Create a `buildSrc/src/main/kotlin/dokka-convention.gradle.kts` file to host the [convention plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:convention_plugins).
5. In the `dokka-convention.gradle.kts` file, add the following snippet:

    ```kotlin
    plugins {
        id("org.jetbrains.dokka") 
    }
    ```

   In the code above, you don't need to specify a Dokka version. The version is set in `buildSrc/build.gradle.kts`   

6. Apply the Dokka convention plugin to your modules (subprojects). Add the convention plugin to each subproject's
   `build.gradle.kts` file:

    ```kotlin
    plugins {
        id("dokka-convention")
    }
    ```

#### Multi-module projects with convention plugins

1. Create a dedicated Dokka [convention plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:convention_plugins) that encapsulates your Dokka [configuration](#adjust-configuration-options),
   such as Dokka source sets, output formats, visibility settings, and more.
2. In the `build.gradle.kts` file of each module (subproject), apply your Dokka convention plugin:
 
   ```kotlin
   plugin {
       id("dokka-convention")
   }
   ```

3. Aggregate the documentation (see below) from multiple modules into a single output. Add dependencies in the root `build.gradle.kts` file
   or in the `build.gradle.kts` file from the aggregating subproject directory, if there's any:

   ```kotlin
   dependencies {
       dokka(project(":parentProject:childProjectA"))
       dokka(project(":parentProject:childProjectB"))
   }
   ```

### Update documentation aggregation in multi-module projects

Dokka can aggregate the documentation from multiple modules (subprojects) into a single output or publication.

As explained in [Multi-module projects with convention plugins](#multi-module-projects-with-convention-plugins),
you must apply the Dokka plugin to all documentable subprojects before aggregating the documentation.

Aggregation in DGP v2 now uses the `dependencies{}` block in any `build.gradle.kts` file instead of tasks.

Previous aggregation:

```kotlin
tasks.dokkaHtmlMultiModule {
  // ...
}
```

New aggregation:

```kotlin
dependencies {
    dokka(project(":some-subproject:"))
    dokka(project(":another-subproject:"))
}
```

### Generate documentation with the updated task

DGP v2 has changed the naming of the Gradle tasks to generate the API documentation.

Previous task:

```kotlin
./gradlew dokkaHtml
    
// OR

./gradlew dokkaHtmlMultiModule
```

New task:

```kotlin
./gradlew :dokkaGenerate
```

In the new version, the `dokkaGenerate` task name works for both single and multi-module projects.

> You can use other tasks to generate output in Javadoc or both HTML and Javadoc. For more information, see
> [Select documentation output format](#select-documentation-output-format).
>
{type="tip"}

After running the `dokkaGenerate` task, the generated API documentation is available in the `build/dokka/` directory.

### Select documentation output format

> The Javadoc output format is in [Alpha](https://kotlinlang.org/docs/components-stability.html#stability-levels-explained).
> You may find bugs and experience migration issues when using it. Successful integration with tools that accept Javadoc
> as input is not guaranteed. Use it at your own risk.
>
{type="note"}

With DGP v2, you can choose to generate the API documentation in HTML, Javadoc, or both formats at the same time:

1. Place the corresponding plugin `id` in the `plugins{}` block of your project's `build.gradle.kts` file:

   ```kotlin
   plugins {
       // Generates HTML documentation
       id("org.jetbrains.dokka") version "2.0.0"
       // OR
       // Generates Javadoc documentation
       id("org.jetbrains.dokka-javadoc") version "2.0.0"
       // Keeping both plugin IDs generates both formats
   }
   ```

2. Run the corresponding Gradle task.

Here's a list of the plugin `id` and Gradle task that correspond to each format:

|             | **HTML**                       | **Javadoc**                         | **Both**                          |
|-------------|--------------------------------|-------------------------------------|-----------------------------------|
| Plugin `id` | `id("org.jetbrains.dokka")`    | `id("org.jetbrains.dokka-javadoc")` | Use both HTML and Javadoc plugins |
| Gradle task | `./gradlew :dokkaGenerateHtml` | `./gradlew :dokkaGenerateJavadoc`   | `./gradlew :dokkaGenerate`        |

### Address deprecations and removals

* **Output format support:** Dokka now only supports HTML and Javadoc output. Experimental formats like Markdown and Jekyll are no longer supported.
* **Collector task:** `DokkaCollectorTask` has been removed. Now, you need to separate the documentation generation for
  each subproject and then [aggregate the documentation](#update-documentation-aggregation-in-multi-module-projects) if necessary.

## Finish up your migration

After you've migrated your project, perform these steps to wrap up and improve performance.

### Set the opt-in flag

After successful migration, set the following opt-in flag without helpers in the project's `gradle.properties` file:

```kotlin
org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled
```

### Enable build cache and configuration cache

DGP v2 now supports Gradle build cache and configuration cache, improving build performance.

* To enable build cache, see [Gradle build cache documentation](https://docs.gradle.org/current/userguide/build_cache.html#sec:build_cache_enable).
* To enable configuration cache, see [Gradle configuration cache documentation](https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:usage:enable ).

## What's next

* Explore more [project examples](https://github.com/Kotlin/dokka/tree/master/examples) using DGP v2. 
* [Get started with Dokka](dokka-get-started.md).
* [Learn more about Dokka plugins](dokka-plugins.md).