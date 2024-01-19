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
}

// Option 1
dependencies {
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")
}

// Option 2
dokka {
    plugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")
}
