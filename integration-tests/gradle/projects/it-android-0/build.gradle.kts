/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.dokka")
    kotlin("android")
}

apply(from = "../template.root.gradle.kts")

android {
    defaultConfig {
        minSdkVersion(21)
        setCompileSdkVersion(29)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
}
