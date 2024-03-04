/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

// The versioning plugin must be applied in all submodules
subprojects {
    repositories {
        mavenCentral()
    }
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.dokka")
    }
    val dokkaPlugin by configurations
    dependencies {
        dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.9.20")
    }
}
