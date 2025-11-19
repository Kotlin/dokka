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
    }
}

//region Workaround
val agpMajorVersion: Int = com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.split('.').first().toInt()
if (agpMajorVersion < 9) {
    dokka.dokkaSourceSets.configureEach {
        enableAndroidDocumentationLink.set(true)
    }
}
