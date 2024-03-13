/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    // include `runner-gradle-plugin`
    includeBuild("../../.")

    dependencyResolutionManagement {
        @Suppress("UnstableApiUsage")
        repositories {
            mavenCentral()
        }
    }
}

rootProject.name = "runner-gradle-plugin-playground-single-module"
