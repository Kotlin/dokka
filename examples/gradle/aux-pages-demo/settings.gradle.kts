pluginManagement {
    val kotlin_version: String by settings
    val dokka_version: String by settings

    plugins {
        kotlin("multiplatform") version kotlin_version
        id("org.jetbrains.dokka") version dokka_version
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    }
}

rootProject.name = "aux-pages-demo"

