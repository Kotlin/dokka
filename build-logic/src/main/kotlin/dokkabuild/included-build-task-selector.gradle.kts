/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import dokkabuild.TaskPathCollector.Companion.IncludedBuildTasksPrefix
import dokkabuild.TaskPathCollector.Companion.SubprojectTasksPrefix
import dokkabuild.TaskPathCollector.Companion.buildLogicIBNames

/**
 * Workaround for https://github.com/gradle/gradle/issues/22335
 */

val taskPathService = gradle.sharedServices.registerIfAbsent("TaskPathCollector", TaskPathCollector::class).get()

taskPathService.addTasksFrom(project)

if (project == rootProject) {
    tasks.addRule("Pattern: ${IncludedBuildTasksPrefix}<TASK>") {
        // Propagate <TASK> to all included builds.
        // (Except build-logic subprojects, because they don't need to be published/tested/etc).
        val taskName = this

        if (taskName.startsWith(IncludedBuildTasksPrefix)) {
            task(taskName) {
                val requestedTaskName = taskName.removePrefix(IncludedBuildTasksPrefix)
                val includedBuildTasks = gradle.includedBuilds
                    .filter { it.name !in buildLogicIBNames }
                    .map { includedBuild ->
                        includedBuild.task(":${SubprojectTasksPrefix}${requestedTaskName}")
                    }
                dependsOn(includedBuildTasks)
            }
        }
    }

    tasks.addRule("Pattern: $SubprojectTasksPrefix<TASK>") {
        // Run all <TASK>s in the current project's subprojects.
        // This should only be run via the 'includedBuildTasks' rule.
        val taskName = this

        if (taskName.startsWith(SubprojectTasksPrefix)) {
            task(taskName) {
                val requestedTaskName = taskName.removePrefix(SubprojectTasksPrefix)
                dependsOn(
                    taskPathService.taskPaths.map { paths ->
                        paths.filter { it.endsWith(":$requestedTaskName") }
                    }
                )
            }
        }
    }
}
