rootProject.name = "dokkatoo-multimodule-example"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        exclusiveContent {
            forRepository {
                maven(providers.gradleProperty("testMavenRepo")) {
                    name = "DokkatooTestMavenRepo"
                }
            }
            filter {
                includeGroup("dev.adamko.dokkatoo")
                includeGroup("dev.adamko.dokkatoo-html")
                includeGroup("dev.adamko.dokkatoo-javadoc")
                includeGroup("dev.adamko.dokkatoo-jekyll")
                includeGroup("dev.adamko.dokkatoo-gfm")
            }
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven(providers.gradleProperty("testMavenRepo")) {
                    name = "DokkatooTestMavenRepo"
                }
            }
            filter {
                includeGroup("dev.adamko.dokkatoo")
                includeGroup("dev.adamko.dokkatoo-html")
                includeGroup("dev.adamko.dokkatoo-javadoc")
                includeGroup("dev.adamko.dokkatoo-jekyll")
                includeGroup("dev.adamko.dokkatoo-gfm")
            }
        }
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
