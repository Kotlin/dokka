import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled

/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
}

dependencies {
    // Classes from src rely on JUnit's @TempDir and Kotlin's @AfterTest,
    // thus these dependencies are needed. Ideally, they should be removed.
    implementation(kotlin("test-junit5"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.eclipse.jgit)
}

kotlin {
    // this project only contains test utils and isn't published, so it doesn't matter about explicit API
    explicitApi = Disabled
}
