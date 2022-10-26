[//]: # (title: Gradle plugin)

To generate documentation for a Gradle-based project, you can use [Dokka Gradle plugin](#applying-the-plugin).

It comes with basic autoconfiguration (including multimodule and multiplatform projects), has convenient 
[Gradle tasks](#generating-documentation) for generating documentation, and provides a great deal of
[configuration options](#configuration) to customize output.

## Applying the plugin

The recommended way of applying the plugin is via 
[plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

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

> Under the hood, Dokka uses [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle.html) to perform autoconfiguration
> of [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) for which documentation
> should be generated, so make sure Kotlin Gradle Plugin is applied as well, or configure source sets manually.
>
{type="note"} 

> If you are using Dokka in a 
> [precompiled script plugin](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins), 
> you will have to add [Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle.html) as a dependency in order for
> it to work properly:
>
> <tabs group="build-script">
> <tab title="Kotlin" group-key="kotlin">
> 
> ```kotlin
> implementation(kotlin("gradle-plugin", "%kotlinVersion%"))
> ```
> 
> </tab>
> <tab title="Groovy" group-key="groovy">
> 
> ```groovy
> implementation 'org.jetbrains.kotlin:kotlin-gradle-plugin:%kotlinVersion%'
> ```
> 
> </tab>
> </tabs>
>
{type="note"}


### Legacy plugin application

If you cannot use plugins DSL for some reason, you can use
[the legacy method](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application) of applying
plugins.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:%dokkaVersion%")
    }
}

apply(plugin="org.jetbrains.dokka")
```

> Note that by applying Dokka this way, certain type-safe accessors will not be available in Kotlin DSL. 
>
{type="note"}

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
buildscript {
    dependencies {
        classpath 'org.jetbrains.dokka:dokka-gradle-plugin:%dokkaVersion%'
    }
}

apply plugin: 'org.jetbrains.dokka'
```

</tab>
</tabs>

## Generating documentation

Dokka's Gradle plugin comes with [HTML](html.md), [Markdown](markdown.md) and [Javadoc](javadoc.md) formats built in,
and adds a number of tasks for generating documentation, both for [single](#single-project-builds) and [multi-project](#multi-project-builds) builds.

### Single project builds

Use the following tasks to build documentation for simple, single project applications and libraries:

| **Task**       | **Description**                                                                     |
|----------------|-------------------------------------------------------------------------------------|
| `dokkaHtml`    | Generates documentation in [HTML](html.md) format.                                  |
| `dokkaGfm`     | Generates documentation in [GitHub Flavored Markdown](markdown.md#gfm) format.      |
| `dokkaJavadoc` | Generates documentation in [Javadoc](javadoc.md) format.                            |
| `dokkaJekyll`  | Generates documentation in [Jekyll compatible Markdown](markdown.md#jekyll) format. |

By default, you will find generated documentation under `build/dokka/{format}` directory of your project.
Output location, among other things, can be [configured](#configuration) separately.

### Multi-project builds

For documenting [multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html), you can
use tasks which are created for all parent projects automatically:

| **Task**                 | **Description**                                                                                  |
|--------------------------|--------------------------------------------------------------------------------------------------|
| `dokkaHtmlMultiModule`   | Generates multi-module documentation in [HTML](html.md) format.                                  |
| `dokkaGfmMultiModule`    | Generates multi-module documentation in [GitHub Flavored Markdown](markdown.md#gfm) format.      |
| `dokkaJekyllMultiModule` | Generates multi-module documentation in [Jekyll compatible Markdown](markdown.md#jekyll) format. |

> [Javadoc](javadoc.md) output format does not have a MultiModule task, but a [Collector](#collector-tasks) task can
> be used instead.
>
{type="note"}

A _MultiModule_ task generates documentation for each subproject individually via [partial](#partial-tasks) tasks,
collects and processes all outputs, and produces complete documentation with a common table of contents and resolved
cross-project references.

By default, you will find ready-to-use documentation under `{parentProject}/build/dokka/{format}MultiModule` directory.

##### MultiModule task example

Given a project with the following structure:

```text
parentProject
    └── childProjectA
        ├── demo
            ├── ChildProjectAClass
    └── childProjectB
        ├── demo
            ├── ChildProjectBClass
```

You will see these pages generated after running `dokkaHtmlMultiModule`:

![Screenshot for output of dokkaHtmlMultiModule task](dokkaHtmlMultiModule-example.png){width=600}

Visit [multi-module project example](https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example)
for more details.

#### Partial tasks

Each subproject will have _partial_ tasks created for it: `dokkaHtmlPartial`,`dokkaGfmPartial`,
and `dokkaJekyllPartial`. 

These tasks are not intended to be used independently and exist only to be called by the parent's MultiModule task.

Output generated by partial tasks contains non-displayable formatting along with unresolved templates and references.

> If you want to generate documentation for a single subproject only, use 
> [single project tasks](#single-project-builds). For instance, `:subproject:dokkaHtml`.

#### Collector tasks

Similar to MultiModule tasks, _Collector_ tasks will be created for each parent project: `dokkaHtmlCollector`, 
`dokkaGfmCollector`, `dokkaJavadocCollector` and `dokkaJekyllCollector`.

A Collector task executes corresponding [single project task](#single-project-builds) for each subproject (for example,
`dokkaHtml`), and merges all outputs into a single virtual project. 

Resulting documentation will look as if you have a single project
build that contains all declarations from the subprojects.

> Use `dokkaJavadocCollector` task if you need to create Javadoc documentation for your multi-project build.
> 
{type="tip"}

#### Collector results

Given a project with the following structure:

```text
parentProject
    └── childProjectA
        ├── demo
            ├── ChildProjectAClass
    └── childProjectB
        ├── demo
            ├── ChildProjectBClass
```

You will see these pages generated after running `dokkaHtmlCollector`:

![Screenshot for output of dokkaHtmlCollector task](dokkaHtmlCollector-example.png){width=800}

Visit [multi-module project example](https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example)
for more details.

## Building javadoc.jar

In order to publish your library to a repository, you may need to provide a `javadoc.jar` file that contains API reference
documentation. 

Dokka's Gradle plugin does not provide any way to do this out of the box, but it can be achieved with custom Gradle
tasks, one for generating documentation in [HTML](html.md) format and another one for [Javadoc](javadoc.md) format:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
tasks.register('dokkaHtmlJar', Jar.class) {
    dependsOn(dokkaHtml)
    from(dokkaHtml)
    archiveClassifier.set("html-docs")
}

tasks.register('dokkaJavadocJar', Jar.class) {
    dependsOn(dokkaJavadoc)
    from(dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
```

</tab>
</tabs>

> If you publish your library to Maven Central, you can use services like [javadoc.io](https://javadoc.io/) to 
> host of your library's API documentation for free and without any setup - it will take documentation pages straight
> from the artifact. It works with both HTML and Javadoc formats as demonstrated by 
> [this example](https://javadoc.io/doc/com.trib3/server/latest/index.html).
> 
{type="tip"}

## Configuration

You can configure tasks/formats individually

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

If you applied Dokka via [plugins DSL](#applying-the-plugin) block:

```kotlin
tasks.dokkaHtml {
    outputDirectory.set(buildDir.resolve("documentation/html"))
}

tasks.dokkaGfm {
    outputDirectory.set(buildDir.resolve("documentation/markdown"))
}

tasks.dokkaHtmlPartial {
    outputDirectory.set(buildDir.resolve("docs/partial"))
}
```

If you applied Dokka with the [buildscript block](#legacy-plugin-application):

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask

tasks.named<DokkaTask>("dokkaHtml") {
    outputDirectory.set(buildDir.resolve("documentation/html"))
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy

dokkaHtml {
    outputDirectory.set(file("build/documentation/html"))
}

dokkaGfm {
    outputDirectory.set(file("build/documentation/markdown"))
}

dokkaHtmlPartial {
    outputDirectory.set(file("build/docs/partial"))
}
```

</tab>
</tabs>

Alternatively, you can configure all tasks/formats at once, including [MultiModule](#multi-project-builds), 
[Partial](#partial-tasks) and [Collector](#collector-tasks) tasks as well. This is often the simplest solution.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.DokkaConfiguration.Visibility

// configure all dokka tasks, including multimodule, partial and collector
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED,
            )
        )

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

// configure partial tasks of all output formats,
// these can have subproject-specific settings
tasks.withType(DokkaTaskPartial::class).configureEach {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

// configure all dokka tasks, including multimodule, partial and collector
tasks.withType(DokkaTask.class) {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set([
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
        ])

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

// configure partial tasks of all output formats, 
// these can have subproject-specific settings
tasks.withType(DokkaTaskPartial.class) {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
    }
}
```

</tab>
</tabs>

### Full configuration

TODO add descriptions for each setting as a separate entry in a table

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform
import java.net.URL

tasks.withType<DokkaTask>().configureEach {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(buildDir.resolve("dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)

    dokkaSourceSets {
        named("customSourceSet") {
            dependsOn("sourceSetDependency")
        }
        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(Visibility.PUBLIC))
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            noStdlibLink.set(false)
            noJdkLink.set(false)
            noAndroidSdkLink.set(false)
            includes.from(project.files(), "packages.md", "extra.md")
            platform.set(Platform.DEFAULT)
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")
            
            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/kotlin/dokka/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/latest/jvm/stdlib/"))
                packageListUrl.set(
                    rootProject.projectDir.resolve("stdlib.package.list").toURL()
                )
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(
                    setOf(
                        Visibility.PUBLIC,
                        Visibility.PRIVATE,
                        Visibility.PROTECTED,
                        Visibility.INTERNAL,
                        Visibility.PACKAGE
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
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform
import java.net.URL

tasks.withType(DokkaTask.class) {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    outputDirectory.set(file("build/dokka/$name"))
    failOnWarning.set(false)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
    offlineMode.set(false)

    dokkaSourceSets {
        named("customSourceSet") {
            dependsOn("sourceSetDependency")
        }
        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set([Visibility.PUBLIC])
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            noStdlibLink.set(false)
            noJdkLink.set(false)
            noAndroidSdkLink.set(false)
            includes.from(project.files(), "packages.md", "extra.md")
            platform.set(Platform.DEFAULT)
            sourceRoots.from(file("src"))
            classpath.from(project.files(), file("libs/dependency.jar"))
            samples.from(project.files(), "samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(file("src"))
                remoteUrl.set(new URL("https://github.com/kotlin/dokka/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLink {
                url.set(new URL("https://kotlinlang.org/api/latest/jvm/stdlib/"))
                packageListUrl.set(
                        file("stdlib.package.list").toURL()
                )
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set([Visibility.PUBLIC])
            }
        }
    }
}
```

</tab>
</tabs>
