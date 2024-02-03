/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

repositories {
    // Remove it when wasm target will be published into public maven repository
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
}

kotlin {
    wasmJs()
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:1.7.2-wasm3")
                implementation("org.jetbrains.kotlinx:atomicfu-wasm-js:0.22.0-wasm2")            }
        }
    }
    wasmWasi()
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url = URL("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
        }
    }
}
