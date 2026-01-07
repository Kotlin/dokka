plugins {
    kotlin("multiplatform") version "2.3.0"
    id("org.jetbrains.dokka") version "2.1.0"
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
