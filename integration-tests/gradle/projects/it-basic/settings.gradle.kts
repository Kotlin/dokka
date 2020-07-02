@file:Suppress("LocalVariableName", "UnstableApiUsage")

pluginManagement {
    val dokka_it_kotlin_version: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm") version dokka_it_kotlin_version
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.dokka") {
                useModule("org.jetbrains.dokka:dokka-gradle-plugin:for-integration-tests-SNAPSHOT")
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

