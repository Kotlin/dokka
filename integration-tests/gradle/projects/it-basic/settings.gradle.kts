@file:Suppress("LocalVariableName", "UnstableApiUsage")

pluginManagement {
    val kotlin_version = "1.3.72"
    val dokka_version = "0.11.0-SNAPSHOT"

    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("org.jetbrains.dokka") version dokka_version
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

