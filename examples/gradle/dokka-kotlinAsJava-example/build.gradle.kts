/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit"))

    // Will apply the plugin to all Dokka tasks
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")

    // Will apply the plugin only to the `:dokkaHtml` task
    //dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")

    // Will apply the plugin only to the `:dokkaGfm` task
    //dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")
}
