/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    `kotlin-dsl`
}

val dokka_it_kotlin_version: String by project
val dokka_it_dokka_version: String by project

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_it_dokka_version")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$dokka_it_kotlin_version")
}
