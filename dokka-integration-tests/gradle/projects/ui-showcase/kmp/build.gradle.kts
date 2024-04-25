/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("uitest.dokka")

    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    macosX64()
    js {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        }
    }
}

tasks.dokkaHtmlPartial {
    dokkaSourceSets.configureEach {
        includes.setFrom("description.md")
    }
}
