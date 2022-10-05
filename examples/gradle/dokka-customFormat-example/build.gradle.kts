import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.7.20"
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
        // Dokka's stylesheets and assets with conflicting names will be overriden.
        // In this particular case, logo-styles.css will be overriden and ktor-logo.png will
        // be added as an additional image asset
        customStyleSheets = listOf(file("logo-styles.css"))
        customAssets = listOf(file("ktor-logo.png"))
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
}
