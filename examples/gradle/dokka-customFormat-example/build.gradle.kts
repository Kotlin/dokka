import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.dokka") version "1.9.0"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.9.0")
    }
}

repositories {
    mavenCentral()
}

tasks.dokkaHtml {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        // Dokka's stylesheets and assets with conflicting names will be overriden.
        // In this particular case, logo-styles.css will be overriden and ktor-logo.png will
        // be added as an additional image asset
        customStyleSheets = listOf(file("logo-styles.css"))
        customAssets = listOf(file("ktor-logo.png"))

        // Text used in the footer
        footerMessage = "(c) Custom Format Dokka example"
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
