/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${System.getenv("DOKKA_VERSION")}")
    }
}

version = "1.9.10-SNAPSHOT"

apply(from = "../template.root.gradle.kts")

tasks.withType<DokkaTask> {
    moduleName.set("Configuration Test Project")
    dokkaSourceSets {
        configureEach {
            failOnWarning.set(project.getBooleanProperty("fail_on_warning"))
            reportUndocumented.set(project.getBooleanProperty("report_undocumented"))
        }
    }
}

fun Project.getBooleanProperty(name: String): Boolean = (project.property(name) as String).toBoolean()
