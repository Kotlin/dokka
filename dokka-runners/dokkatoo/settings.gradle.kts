rootProject.name = "dokkatoo"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {

  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

  repositories {
    mavenCentral()
    google()

    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://www.myget.org/F/rd-snapshots/maven/")

    ivy("https://github.com/") {
      name = "GitHub Release"
      patternLayout {
        artifact("[organization]/[module]/archive/[revision].[ext]")
        artifact("[organization]/[module]/archive/refs/tags/[revision].[ext]")
        artifact("[organization]/[module]/archive/refs/tags/v[revision].[ext]")
      }
      metadataSources { artifact() }
    }
  }
}

include(
  ":examples",

  ":modules:docs",
  ":modules:dokkatoo-plugin",
  ":modules:dokkatoo-plugin-integration-tests",
)


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")


//if (file("./examples/build/tmp/prepareDokkaSource").exists()) {
//  includeBuild("./examples/build/tmp/prepareDokkaSource")
//}

// can only include one example project at a time https://github.com/gradle/gradle/issues/23939
//@formatter:off
//includeBuild(file("./examples/multiplatform-example/dokkatoo"                                                 )) { name = "multiplatform-example"          }
//includeBuild(file("./examples/kotlin-as-java-example/dokkatoo"                                                )) { name = "kotlin-as-java-example"         }
//includeBuild(file("./examples/versioning-multimodule-example/dokkatoo"                                        )) { name = "versioning-multimodule-example" }
//includeBuild(file("./examples/custom-format-example/dokkatoo"                                                 )) { name = "custom-format-example"          }
//includeBuild(file("./examples/gradle-example/dokkatoo"                                                        )) { name = "gradle-example"                 }
//includeBuild(file("./examples/library-publishing-example/dokkatoo"                                            )) { name = "library-publishing-example"     }
//includeBuild(file("./examples/multimodule-example/dokkatoo"                                                   )) { name = "multimodule-example"            }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-multimodule-1/dokkatoo"            )) { name = "it-multimodule-1"               }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-multimodule-0/dokkatoo"            )) { name = "it-multimodule-0"               }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-collector-0/dokkatoo"              )) { name = "it-collector-0"                 }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-multimodule-versioning-0/dokkatoo" )) { name = "it-multimodule-versioning-0"    }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-android-0/dokkatoo"                )) { name = "it-android-0"                   }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-basic/dokkatoo"                    )) { name = "it-basic"                       }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-multiplatform-0/dokkatoo"          )) { name = "it-multiplatform-0"             }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-js-ir-0/dokkatoo"                  )) { name = "it-js-ir-0"                     }
//includeBuild(file("./modules/dokkatoo-plugin-integration-tests/projects/it-basic-groovy/dokkatoo"             )) { name = "it-basic-groovy"                }
//@formatter:on

//listOf(
//  "examples",
//  "modules/dokkatoo-plugin-integration-tests/projects",
//).forEach { exampleProjectDir ->
//  file(exampleProjectDir)
//    .walk()
//    .filter {
//      it.isDirectory
//          && it.name == "dokkatoo"
//          && (
//          it.resolve("settings.gradle.kts").exists()
//              ||
//              it.resolve("settings.gradle").exists()
//          )
//    }.forEach { file ->
//      includeBuild(file) {
//        name = file.parentFile.name
//        println("$file $name")
//      }
//    }
//}
