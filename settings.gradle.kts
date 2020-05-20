rootProject.name = "dokka"

include("core")
include("plugins:base:search-component")
include("coreDependencies")
include("testApi")
include("test-tools")
include("runners:gradle-plugin")
include("runners:cli")
include("runners:maven-plugin")
include("plugins:base")
include("plugins:base:frontend")
include("plugins:mathjax")
include("plugins:gfm")
include("plugins:jekyll")
include("plugins:kotlin-as-java")
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
        maven(url="https://dl.bintray.com/kotlin/kotlin-dev/")
        mavenLocal()
        mavenCentral()
        jcenter()
        gradlePluginPortal()
    }
}