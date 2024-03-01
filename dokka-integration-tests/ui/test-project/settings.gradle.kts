@file:Suppress("UnstableApiUsage")

/*
* Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
*/

rootProject.name = "dokka-ui-test-project"

pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test")
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test")
        mavenLocal()
    }
}

include(
    ":jvm",
    ":kmp"
)
