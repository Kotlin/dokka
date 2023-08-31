/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    // Classes from src rely on JUnit's @TempDir and Kotlin's @AfterTest,
    // thus these dependencies are needed. Ideally, they should be removed.
    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.eclipse.jgit)
}
