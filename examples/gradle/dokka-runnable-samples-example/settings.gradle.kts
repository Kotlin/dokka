/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "dokka-runnable-samples-example"

pluginManagement {
    repositories {
        mavenLocal() // TODO: only for reproducer
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal() // TODO: only for reproducer
        mavenCentral()
    }
}
