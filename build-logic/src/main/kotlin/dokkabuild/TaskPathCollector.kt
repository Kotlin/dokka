/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import javax.inject.Inject

abstract class TaskPathCollector @Inject constructor(
    private val providers: ProviderFactory,
) : BuildService<BuildServiceParameters.None> {
    /** Full paths of all subproject tasks. */
    abstract val taskPaths: SetProperty<String>

    fun addTasksFrom(project: Project) {
        val projectPath = providers.provider { project.path }
        val taskNames = providers.provider { project.tasks.names }

        taskPaths.addAll(
            taskNames.zip(projectPath) { names, path ->
                names.map { name -> "$path$name" }
            }
        )
    }

    @Suppress("ConstPropertyName")
    companion object {
        // TODO rename it's not just subproject tasks, it's all tasks in the build
        const val SubprojectTasksPrefix = "subprojectTasks_"
        const val IncludedBuildTasksPrefix = "includedBuildTasks_"

        val buildLogicProjectsNames = setOf(
            "build-logic",
            "build-settings-logic",
        )

        /** Only call this in the root project, otherwise different IBs depends on other IBs, and it gets messy. */
        fun Task.dependsOnIncludedBuildTasks(
            taskName: String = name
        ) {
            dependsOn("$IncludedBuildTasksPrefix$taskName")
        }
    }
}
