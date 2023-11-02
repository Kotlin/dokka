/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:1.7.2-wasm1")
                implementation("org.jetbrains.kotlinx:atomicfu-wasm-js:0.22.0-wasm1")            }
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

// HACK: some dependencies (coroutines -wasm0 and atomicfu -wasm0) reference deleted *-dev libs
configurations.all {
    val conf = this
    resolutionStrategy.eachDependency {
        // for  atomicfu-wasm-js:0.22.0-wasm1 and kotlinx-coroutines-core:1.6.4-wasm1
        if (requested.version == "1.9.30-dev-460") {
            println("Substitute deleted version ${requested.module}:${requested.version} for ${conf.name}")
            useVersion(project.properties["dokka_it_kotlin_version"] as String)
        }
    }
}
