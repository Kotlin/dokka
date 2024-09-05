plugins {
    kotlin("jvm") apply false
    `dokka-convention`
}

dependencies {
    dokka(project(":childProjectA"))
    dokka(project(":childProjectB"))

    // No version is necessary, Dokka will add it automatically
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin")
}

dokka {
    moduleName.set("Dokka Versioning Example")
}

val currentVersion = "1.0"
val previousVersionsDirectory: Directory = layout.projectDirectory.dir("previousDocVersions")

dokka {
    pluginsConfiguration {
        // Main configuration for the versioning plugin:
        versioning {
            // Generate documentation for the current version of the application.
            version = currentVersion

            // Look for previous versions of docs in the directory defined in
            // `previousVersionsDirectory` allowing it to create the version
            // navigation dropdown menu.
            olderVersionsDir = previousVersionsDirectory
        }
    }
}
