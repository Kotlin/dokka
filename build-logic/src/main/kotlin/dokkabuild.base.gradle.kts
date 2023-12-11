/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.DokkaBuildProperties
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 *
 * It provides the [DokkaBuildProperties] extension, for accessing common build properties.
 */

plugins {
    base
}

extensions.create<DokkaBuildProperties>(DokkaBuildProperties.EXTENSION_NAME)

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}


val integrationTestPreparation by tasks.registering {
    description = "lifecycle task for preparing the project for integration tests"
    group = VERIFICATION_GROUP
}

if (project == rootProject) {
    // workaround for running all tasks in an included build https://github.com/gradle/gradle/issues/22335
    tasks.addRule("Pattern: allProjectTasks_<TASK>") {
        val taskName = this
        val taskPrefix = "allProjectTasks_"

        if (startsWith(taskPrefix)) {
            task(taskName) {
                val buildTaskName = taskName.removePrefix(taskPrefix).replaceFirstChar(Char::lowercase)
                val allProjectsTasks = allprojects
                    .filter { it.projectDir.resolve("build.gradle.kts").exists() }
                    .map { subproject -> "${subproject.path}:$buildTaskName" }
                dependsOn(allProjectsTasks)
            }
        }
    }
}
