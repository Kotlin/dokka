plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.dokka") version "2.0.20-SNAPSHOT"
}

dokka {
    moduleName.set("customFormat-example")
    pluginsConfiguration.html {
        // Dokka's stylesheets and assets with conflicting names will be overridden.
        // In this particular case, logo-styles.css will be overridden
        // and ktor-logo.png will be added as an additional image asset
        customStyleSheets.from("logo-styles.css")
        customAssets.from("ktor-logo.png")

        // Text used in the footer
        footerMessage.set("(c) Custom Format Dokka example")
    }
}
