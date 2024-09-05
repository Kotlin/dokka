/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "versioning-multimodule-example"

pluginManagement {
    repositories {
        gradlePluginPortal()
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

include(
    ":docs",
    ":childProjectA",
    ":childProjectB",
)
