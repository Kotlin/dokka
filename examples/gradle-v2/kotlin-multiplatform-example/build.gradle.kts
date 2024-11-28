plugins {
    kotlin("multiplatform") version "1.9.25"
    id("org.jetbrains.dokka") version "2.0.20-SNAPSHOT"
}

group = "org.dokka.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()

    js(IR) {
        browser()
    }

    macosX64()
    macosArm64()

    iosX64()
    iosArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
    }
}

dokka {
    // Dokka can be configured here
}
