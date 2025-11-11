/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("com.android.kotlin.multiplatform.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    kotlin("multiplatform") version "/* %{KGP_VERSION} */"
}

kotlin {
    androidLibrary {
        namespace = "com.example.kmpfirstlib"
        compileSdk = 33
        minSdk = 24

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation("androidx.appcompat:appcompat:1.1.0")
            }
        }
        getByName("androidHostTest") {
            dependencies {
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }
    }
}
