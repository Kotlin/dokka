import org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS

/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "hide-internal-api"

pluginManagement {
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = PREFER_SETTINGS
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}