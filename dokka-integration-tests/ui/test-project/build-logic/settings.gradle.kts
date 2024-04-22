rootProject.name = "build-logic"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test")
        mavenLocal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test")
        mavenLocal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
