/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.DokkaConfiguration
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${System.getenv("DOKKA_VERSION")}")
    }
}

fun createTask(name: String) {
    tasks.register(name, org.jetbrains.dokka.gradle.DokkaTask::class) {
        dokkaSourceSets {
            moduleName.set("Some example")
            // create a new source set
            register("kotlin-stdlib-common") {
                sourceRoots.from("src/common/java")
                sourceRoots.from("src/common/kotlin")
                samples.from("src/common/kotlin")
            }
        }
    }
}

task("runTasks") {
    val taskNumber = (properties["task_number"] as String).toInt()
    repeat(taskNumber) { i ->
        createTask("task_"+i)
        dependsOn ("task_"+i)
    }
}
