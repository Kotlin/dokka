## Changes between 0.10.x and 0.11.0

There are two main changes between dokka 0.10.x and 0.11.0

The first is the introduction of plugability - new documentation creating process is divided into several steps and each step provides extension points to be used. To learn more about new dokka pipeline and possible plugins, please read Developer's guide.

Second difference comes with the change with the subject of dokka pass. Previously, separate dokka passes where set for every targeted platform, now every source set has its own pass and the name itself changed to `sourceSet`.

### Gradle

With changing the approach from platform-based to source-set-based, we replace both `configuration` and `multiplatform` blocks with `dokkaSourceSets`. It's still a collection of dokka passes configuration, so the structure stays as it was.
Format selection is now done using plugins with dokka providing preconfigured tasks for different formats: `dokkaHtml`, `dokkaJavadoc`, `dokkaGfm` and `dokkaJekyll`.

* `moduleName` has changed to `moduleDisplayName`
* `targets` has been dropped. Declaration merging is now done by the source set mechanism. Name customization can be done using `displayName` property
* `outputFormat` has been dropped. Format can be selected with appropriate plugins, please refer to the README

#### Groovy
##### Old
```groovy
dokka {
    outputFormat = 'html'
    outputDirectory = "$buildDir/dokka"
    multiplatform {
        js {
            includes = ["src/jsMain/resources/doc.md"]
            samples = ["src/jsMain/resources/Samples.kt"]
            sourceLink {
                path = "src/jsMain/kotlin"
                url = "https:/dokka.documentation.com/jsMain/kotlin"
                lineSuffix = "#L"
            }
        }
        jvm {
            includes = ["src/jvmMain/resources/doc.md"]
            samples = ["src/jsMain/resources/Samples.kt"]
            sourceLink {
                path = "src/jvmMain/kotlin"
                url = "https:/dokka.documentation.com/jvmMain/kotlin"
                lineSuffix = "#L"
            }
        }
}
```
##### New
```groovy
dokkaHtml { // or dokkaGfm, dokkaJekyll, ...
    outputDirectory = "$buildDir/dokka"

    dokkaSourceSets {
        commonMain {}
        jsMain {
            includes = ["src/jsMain/resources/doc.md"]
            samples = ["src/jsMain/resources/Samples.kt"]
            sourceLink {
                path = "src/jsMain/kotlin"
                url = "https:/dokka.documentation.com/jsMain/kotlin"
                lineSuffix = "#L"
            }
        }

        jvmMain {
            includes = ["src/jvmMain/resources/doc.md"]
            samples = ["src/jsMain/resources/Samples.kt"]
            sourceLink {
                path = "src/jvmMain/kotlin"
                url = "https:/dokka.documentation.com/jvmMain/kotlin"
                lineSuffix = "#L"
            }
        }
    }
}
```

#### Kotlin

##### Old
```kotlin
kotlin {  // Kotlin Multiplatform plugin configuration
    jvm()
    js("customName")
}

val dokka by getting(DokkaTask::class) {
    outputDirectory = "$buildDir/dokka"
    outputFormat = "html"

    multiplatform { 
        val customName by creating { // The same name as in Kotlin Multiplatform plugin, so the sources are fetched automatically
            includes = listOf("packages.md", "extra.md")
            samples = listOf("samples/basic.kt", "samples/advanced.kt")
        }

        register("differentName") { // Different name, so source roots must be passed explicitly
            targets = listOf("JVM")
            platform = "jvm"
            sourceRoot {
                path = kotlin.sourceSets.getByName("jvmMain").kotlin.srcDirs.first().toString()
            }
            sourceRoot {
                path = kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs.first().toString()
            }
        }
    }
}
```

##### New
```kotlin
kotlin {  // Kotlin Multiplatform plugin configuration
    jvm()
    js("customName")
}

dokkaHtml { // or dokkaGfm, dokkaJekyll, ...
    outputDirectory = "$buildDir/dokka"

    dokkaSourceSets { 
        val customNameMain by creating { // The same source set name as in Kotlin Multiplatform plugin, so the sources are fetched automatically
            includes = listOf("packages.md", "extra.md")
            samples = listOf("samples/basic.kt", "samples/advanced.kt")
        }
        
        val commonMain by creating {} // Document commonj source set

        create("differentName") { // Different name, so source roots must be passed explicitly
            sourceSetName = listOf("JVM")
            platform = "jvm"
            sourceRoot {
                path = kotlin.sourceSets.getByName("jvmMain").kotlin.srcDirs.first().toString()
            }
            dependsOn(commonMain) // The jvm source set depends on the common source set 
        }
    }
}
```

#### Multimodule page

There is a new task, `dokkaMultimodule`, that creates an index page for all documented subprojects. It is available only for top-level project.

```groovy
dokkaMultimodule {
    // File to be used as an excerpt for each subprojects
    documentationFileName = "README.md"
    // output format for rendering multimodule page (accepts the same values as regular dokka task)
    outputFormat = "html"
    // output directory for page
    outputDirectory = "$buildDir/dokka"
}
```

### Maven

There are no changes in maven configuration API for dokka, all previous configurations should work without issues.
The default platform label is "JVM", `sourceSetName` can be used to change it.

### Ant
Support for the Ant plugin has been dropped, dokka should be used with CLI in Ant instead.  
 
### Command Line 
Dokka fajtar has been dropped, thus the command line interface has changed slightly.
Most importantly, all plugins and their dependencies have to be provided in the `-pluginsClasspath` argument (paths are seperated with ';').
A build tool like Gradle or Maven is recommended to resolve and download all required artifacts.
Instead of creating a long configuration command, dokka can be configured with a JSON file. Please refer to the README.