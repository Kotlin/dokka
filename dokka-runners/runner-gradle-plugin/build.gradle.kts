/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.20"
}

kotlin {
    explicitApi()
    jvmToolchain(8)

    @Suppress("DEPRECATION")
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_1_4
        apiVersion = KotlinVersion.KOTLIN_1_4
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.android)

    testImplementation(libs.gradlePlugin.kotlin)
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    plugins {
        create("dokka") {
            id = "org.jetbrains.dokka2"
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"

            displayName = "Dokka plugin"
            description = "Dokka is an API documentation engine for Kotlin"
            tags.addAll("dokka", "kotlin", "kdoc", "android", "documentation", "api")
        }
    }
}
