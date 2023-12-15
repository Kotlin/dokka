/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("LocalVariableName", "UnstableApiUsage")

pluginManagement {
    val dokka_it_kotlin_version: String by settings
    //val dokka_it_dokka_version: String by settings
    val dokka_it_android_gradle_plugin_version: String? by settings

    plugins {
        id("org.jetbrains.kotlin.js") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.jvm") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.android") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.multiplatform") version dokka_it_kotlin_version
        // only one repository should provide Dokka, and that's a project-local directory,
        // so just use the latest Dokka version
        id("org.jetbrains.dokka") version "+"
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.library") {
                useModule("com.android.tools.build:gradle:$dokka_it_android_gradle_plugin_version")
            }

            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:$dokka_it_android_gradle_plugin_version")
            }
        }
    }
    repositories {
        /* %{PROJECT_LOCAL_MAVEN_DIR}% */
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        /* %{PROJECT_LOCAL_MAVEN_DIR}% */
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }
        // Remove when Kotlin/Wasm is published into public Maven repository
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}
