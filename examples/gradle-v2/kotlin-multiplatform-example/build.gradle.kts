plugins {
    kotlin("multiplatform") version "2.3.0"
    id("org.jetbrains.dokka") version "2.1.0"
}

group = "org.dokka.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()

    js {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks {
            register("kotlinx.coroutines") {
                url("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
        }
    }
}
