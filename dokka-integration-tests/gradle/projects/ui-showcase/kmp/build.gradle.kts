/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("uitest.dokka-kmp")

    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    macosX64()

    // adding linuxArm64 and macosArm64 is a workaround for https://github.com/Kotlin/dokka/issues/3386
    linuxArm64()
    macosArm64()

    js {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        }
    }

    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }
    }

}

dokka {
    dokkaPublications.html {
        includes.setFrom("description.md")
    }
}
