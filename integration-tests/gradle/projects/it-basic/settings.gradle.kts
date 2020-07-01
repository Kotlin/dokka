@file:Suppress("LocalVariableName", "UnstableApiUsage")

pluginManagement {
    val kotlin_version: String by settings
    val dokka_version: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.dokka") {
                useModule("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_version")
            }
        }
    }

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-dev/")
        mavenLocal()
        mavenCentral()
        jcenter()
        gradlePluginPortal()
    }
}

rootProject.name = "it-basic"
