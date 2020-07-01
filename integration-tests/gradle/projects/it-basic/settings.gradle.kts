@file:Suppress("LocalVariableName", "UnstableApiUsage")

pluginManagement {
    val dokka_it_kotlin_version: String by settings
    val dokka_version = "0.11.0-SNAPSHOT"

    plugins {
        id("org.jetbrains.kotlin.jvm") version dokka_it_kotlin_version
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

