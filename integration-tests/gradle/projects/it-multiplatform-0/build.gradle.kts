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

kotlin {
    jvm()
    linuxX64("linux")
    macosX64("macos")
    js(BOTH)
    //TODO Add wasm when kx.coroutines will be supported and published into the main repo
    sourceSets {
        val commonMain by sourceSets.getting
        val linuxMain by sourceSets.getting
        val macosMain by sourceSets.getting
        val desktopMain by sourceSets.creating {
            dependsOn(commonMain)
            linuxMain.dependsOn(this)
            macosMain.dependsOn(this)
        }
        named("commonMain") {
            dependencies {
                if (properties["dokka_it_kotlin_version"] in listOf("1.4.32", "1.5.31"))
                    // otherwise for a modern versin of coroutines:
                    // Failed to resolve Kotlin library: project/build/kotlinSourceSetMetadata/commonMain/org.jetbrains.kotlinx-kotlinx-coroutines-core/org.jetbrains.kotlinx-kotlinx-coroutines-core-commonMain.klib
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
                else
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
                //packageListUrl.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/package-list"))
            }
        }
    }
}
