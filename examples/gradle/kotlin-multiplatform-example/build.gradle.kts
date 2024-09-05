plugins {
    kotlin("multiplatform") version "1.9.22"
    id("dev.adamko.dokkatoo") version "2.4.0-SNAPSHOT"
}

group = "org.dokka.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm() // Creates a JVM target with the default name "jvm"
    linuxX64("linux")
    macosX64("macos")
    js(IR) {
        browser()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
            }
        }
    }
}

dokka {
    // Create a custom source set not known to the Kotlin Gradle Plugin
    dokkatooSourceSets.register("customSourceSet") {
        jdkVersion.set(9)
        displayName.set("custom")
        sourceRoots.from("src/customJdk9/kotlin")
    }
}


//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokka {
    sourceSetScopeDefault.set(":dokkaHtml")
    dokkatooSourceSets.matching { it.name != "customSourceSet" }.configureEach {
        jdkVersion.set(8)
    }
}
//endregion
