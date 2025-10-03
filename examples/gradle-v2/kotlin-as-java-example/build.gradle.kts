/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm") version "2.2.21-RC"
    id("org.jetbrains.dokka") version "2.1.0-SNAPSHOT"
}

dependencies {
    testImplementation(kotlin("test-junit"))

    // Will apply the plugin only to the `:dokkaHtml` task
    // (Dokka will automatically add the version)
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin")
}
