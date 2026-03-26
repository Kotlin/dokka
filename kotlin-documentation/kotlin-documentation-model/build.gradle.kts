/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinxSerialization)
    id("dokkabuild.publish-base")
}

kotlin {
    explicitApi()
    jvmToolchain(8)

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    js {
        browser()
        nodejs()
    }
    // all other targets ...

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.kotlinxSerialization.bom))
            implementation(libs.kotlinxSerialization.json)
            implementation(libs.kotlinxSerialization.cbor)
            implementation(libs.kotlinxSerialization.protobuf)
        }
    }
}
