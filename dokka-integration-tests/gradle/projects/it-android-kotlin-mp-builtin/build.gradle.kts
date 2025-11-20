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

val agpMajorVersion: Int = com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.split('.').first().toInt()
if (agpMajorVersion < 9) {
    // AGP 8 uses KotlinTarget JVM instead of AndroidJVM.
    // This is an AGP 8 bug - it's fixed in AGP 9.
    // For AGP <9, workaround the bug by manually enabling the Android documentation link.
    dokka.dokkaSourceSets.configureEach {
        enableAndroidDocumentationLink.set(true)
    }
}
