## Changes between 0.10.x and 0.11.0

There are two main changes between dokka 0.10.x and 0.11.0

The first is the introduction of plugability - new documentation creating process is divided into several steps and each step provides extension points to be used. To learn more about new dokka pipeline and possible plugins, please read Developer's guide.

Second difference comes with the change with the subject of dokka pass. Previously, separate dokka passes where set for every targeted platform, now every source set has its own pass.

### Gradle

With changing the approach from platform-based to sourceset-based, we replace `multiplatform` block with `dokkaSourceSets`. It's still a collection of dokka passes configuration, so the structure stays as it was.

#### Old
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
}
```
#### New
```groovy
    dokka {
        outputFormat = 'html'
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

            val jvmMain by creating {
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

