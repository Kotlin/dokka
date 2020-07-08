rootProject.name = "dokka"

include("core")
include("core:dependencies")
include("plugins:base:search-component")
include("testApi")
include("test-tools")
include("runners:gradle-plugin")
include("runners:cli")
include("runners:maven-plugin")
include("kotlin-analysis")
include("kotlin-analysis:dependencies")
include("plugins:base")
include("plugins:base:frontend")
include("plugins:base:test-utils")
include("plugins:mathjax")
include("plugins:gfm")
include("plugins:jekyll")
include("plugins:kotlin-as-java")
include("plugins:javadoc")
include("integration-tests")
include("integration-tests:gradle")
include("integration-tests:cli")

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.github.johnrengelman.shadow") version "5.2.0"
        id("com.jfrog.bintray") version "1.8.5"
        id("com.gradle.plugin-publish") version "0.10.1"
    }

    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev/")
        mavenLocal()
        mavenCentral()
        jcenter()
        gradlePluginPortal()
    }
}
