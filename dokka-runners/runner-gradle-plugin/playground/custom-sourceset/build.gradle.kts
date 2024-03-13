/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDelicateApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("org.jetbrains.dokka")
    kotlin("jvm") version "1.9.20"
}

dokka {
    currentProject {
        @OptIn(DokkaGradlePluginDelicateApi::class)
        sourceSets.register("custom") {
            platform = KotlinPlatformType.jvm
            languageVersion = KotlinVersion.KOTLIN_2_1
            apiVersion = KotlinVersion.KOTLIN_2_1
            classpath.from(layout.projectDirectory.dir("kotlin"))
            // and so on
        }
    }
}
