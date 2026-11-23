/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

kotlin {
    wasmJs()
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                // Use the newest kotlinx library versions that are compatible with the
                // tested Kotlin Gradle Plugin version (libraries are built with newer
                // Kotlin versions, and old compilers cannot read newer metadata).
                val kotlinVersion = properties["dokka_it_kotlin_version"].toString()
                val coroutinesVersion = when {
                    kotlinVersion >= "2.1.0" -> "1.11.0" // built with Kotlin 2.2.20
                    kotlinVersion >= "2.0.0" -> "1.9.0"   // built with Kotlin 2.0.0
                    else -> "1.8.0"                      // built with Kotlin 1.9.x
                }
                val atomicfuVersion = when {
                    kotlinVersion >= "2.1.0" -> "0.33.0"  // built with Kotlin 2.2.21
                    kotlinVersion >= "2.0.0" -> "0.28.0"  // built with Kotlin 2.1.0
                    else -> "0.23.0"                     // built with Kotlin 1.9.x
                }
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:atomicfu-wasm-js:$atomicfuVersion")
            }
        }
    }
    wasmWasi()
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
            }
        }
    }
}
