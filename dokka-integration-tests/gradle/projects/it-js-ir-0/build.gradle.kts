/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
    kotlin("js")
}

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
}

dependencies {
    implementation(npm("is-sorted", "1.0.5"))

    val reactVersion = properties["react_version"]
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")
}
