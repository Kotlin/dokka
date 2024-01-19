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

dokka {
    moduleName.set("Dokka Gradle Example")
    includes.from("Module.md")
    sourceLink("https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example")
}
