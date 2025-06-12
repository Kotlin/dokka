/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.work.DisableCachingByDefault

private const val DEPRECATION_MESSAGE = """
    It is an anti-pattern to declare cross-project dependencies as it leads to various build problems. 
    For this reason, this API wil be removed with the introduction of project isolation. 
    When it happens, we will provide a migration guide. In the meantime, you can keep using this API
    if you have to, but please don't rely on it if possible. If you don't want to document a certain project,
    don't apply the Dokka plugin for it, or disable individual project tasks using the Gradle API .
"""

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
abstract class AbstractDokkaParentTask : @Suppress("DEPRECATION") AbstractDokkaTask() {

    @get:Internal
    internal var childDokkaTaskPaths: Set<String> = emptySet()
        private set

    @get:Nested
    internal val childDokkaTasks: Set<@Suppress("DEPRECATION") AbstractDokkaTask>
        get() = childDokkaTaskPaths
            .mapNotNull { path -> project.tasks.findByPath(path) }
            .map(::checkIsAbstractDokkaTask)
            .toSet()

    /* By task reference */
    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addChildTask(task: @Suppress("DEPRECATION") AbstractDokkaTask) {
        childDokkaTaskPaths = childDokkaTaskPaths + task.path
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTask(task:@Suppress("DEPRECATION")  AbstractDokkaTask) {
        childDokkaTaskPaths = childDokkaTaskPaths - task.path
    }

    /* By path */
    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addChildTask(path: String) {
        childDokkaTaskPaths = childDokkaTaskPaths + project.absoluteProjectPath(path)
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTask(path: String) {
        childDokkaTaskPaths = childDokkaTaskPaths - project.absoluteProjectPath(path)
    }

    /* By project reference and name */
    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            @Suppress("DEPRECATION")
            addChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            @Suppress("DEPRECATION")
            removeChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addSubprojectChildTasks(childTasksName: String) {
        @Suppress("DEPRECATION")
        addChildTasks(project.subprojects, childTasksName)
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeSubprojectChildTasks(childTasksName: String) {
        @Suppress("DEPRECATION")
        removeChildTasks(project.subprojects, childTasksName)
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTasks(project: Project) {
        childDokkaTaskPaths = childDokkaTaskPaths.filter { path ->
            parsePath(path).parent != parsePath(project.path)
        }.toSet()
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTasks(projects: Iterable<Project>) {
        projects.forEach { project ->
            @Suppress("DEPRECATION")
            removeChildTasks(project)
        }
    }

    private fun checkIsAbstractDokkaTask(task: Task): @Suppress("DEPRECATION") AbstractDokkaTask {
        if (task is @kotlin.Suppress("DEPRECATION") AbstractDokkaTask) {
            return task
        }
        throw IllegalArgumentException(
            "Only tasks of type ${@Suppress("DEPRECATION") AbstractDokkaTask::class.java.name} can be added as child for " +
                    "${@Suppress("DEPRECATION") AbstractDokkaParentTask::class.java.name} tasks.\n" +
                    "Found task ${task.path} of type ${task::class.java.name} added to $path"
        )
    }
}
