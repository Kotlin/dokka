plugins {
    kotlin("jvm") apply false
    `dokka-convention`
}

dependencies {
    dokkatoo(project(":parentProject:childProjectA"))
    dokkatoo(project(":parentProject:childProjectB"))

    dokkatooPluginHtml("org.jetbrains.dokka:versioning-plugin") // Dokkatoo will automatically add the version
}

dokka {
    moduleName.set("Dokka Versioning Example")
}

val currentVersion = "1.0"
val previousVersionsDirectory: Directory =
    rootProject.layout.projectDirectory.dir("previousDocVersions")

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
