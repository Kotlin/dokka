rootProject.name = "dokka"

include("core")
include("core:test-api")
include("core:content-matcher-test-utils")
include("runners:gradle-plugin")
include("runners:cli")
include("runners:maven-plugin")

include("plugins:base")
include("plugins:base:frontend")
include("plugins:base:search-component")
include("plugins:base:base-test-utils")

include("plugins:kotlin-analysis")
include("plugins:kotlin-analysis:intellij-dependency")
include("plugins:kotlin-analysis:compiler-dependency")

include("plugins:parsers")
include("plugins:kotlin-documentables")
include("plugins:java-documentables")
include("plugins:processing")
include("plugins:location")
include("plugins:all-module-page")

include("plugins:rendering")
include("plugins:html")
include("plugins:gfm")
include("plugins:jekyll")

include("plugins:mathjax")
include("plugins:kotlin-as-java")
include("plugins:javadoc")

include("integration-tests")
include("integration-tests:gradle")
include("integration-tests:cli")
include("integration-tests:maven")

include("test-utils")

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.github.johnrengelman.shadow") version "5.2.0"
        id("com.jfrog.bintray") version "1.8.5"
        id("com.gradle.plugin-publish") version "0.12.0"
    }

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap/")
        maven("https://dl.bintray.com/kotlin/kotlin-dev/")
        mavenCentral()
        jcenter()
        gradlePluginPortal()
    }
}
