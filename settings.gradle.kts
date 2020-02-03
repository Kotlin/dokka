rootProject.name = "dokka"

include("core")
include("coreDependencies")
include("testApi")
include("runners:gradle-plugin")
include("runners:cli")
include("runners:maven-plugin")
include("plugins:xml")
include("plugins:mathjax")
include("integration-tests:gradle-integration-tests")


pluginManagement {
    val kotlin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.github.johnrengelman.shadow") version "5.2.0"
        id("com.jfrog.bintray") version "1.8.4"
        id("com.gradle.plugin-publish") version "0.10.1"
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        gradlePluginPortal()
    }
}