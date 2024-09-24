/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.dokka") version "2.0.20-SNAPSHOT"
}

dependencies {
    testImplementation(kotlin("test-junit"))

    // Will apply the plugin only to the `:dokkaHtml` task
    // (Dokka will automatically add the version)
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin")
}
