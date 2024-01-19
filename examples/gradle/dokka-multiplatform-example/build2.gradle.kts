/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform

plugins {
    kotlin("multiplatform") version "1.9.10"
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    mavenCentral()
}

group = "org.dokka.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm() // Creates a JVM target with the default name "jvm"
    linuxX64("linux")
    macosX64("macos")
    js()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
            }
        }
    }
}

dokka {
    sourceSets.register("customSourceSet") {
        displayName.set("custom")
        jdkVersion.set(9)
        sourceRoots.from(file("src/customJdk9/kotlin"))
    }
}
