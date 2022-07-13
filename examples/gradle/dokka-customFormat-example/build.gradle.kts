import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.dokka") version ("1.7.10")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.7.10")
    }
}

repositories {
    mavenCentral()
}

/**
 * Custom format adds a custom logo
 */
tasks.register<DokkaTask>("dokkaCustomFormat") {
    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
        customStyleSheets = listOf(file("logo-styles.css"))
        customAssets = listOf(file("ktor-logo.png"))
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
}
