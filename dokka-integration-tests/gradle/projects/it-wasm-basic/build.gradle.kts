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
    wasm()
    sourceSets {
        val wasmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4-wasm1")
                implementation("org.jetbrains.kotlinx:atomicfu-wasm:0.18.5-wasm1")
            }
        }
    }
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

// HACK: some dependencies (coroutines -wasm0 and atomicfu -wasm0) reference deleted *-dev libs
configurations.all {
    val conf = this
    resolutionStrategy.eachDependency {
        if (requested.version == "1.8.20-dev-3308") {
            println("Substitute deleted version ${requested.module}:${requested.version} for ${conf.name}")
            useVersion(project.properties["dokka_it_kotlin_version"] as String)
        }
    }
}
