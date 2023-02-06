package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Nested
import org.gradle.work.DisableCachingByDefault

private const val DEPRECATION_MESSAGE = """
    It is an anti-pattern to declare cross-project dependencies as it leads to various build problems. 
    For this reason, this API wil be removed with the introduction of project isolation. 
    When it happens, we will provide a migration guide. In the meantime, you can keep using this API
    if you have to, but please don't rely on it if possible. If you don't want to document a certain project,
    don't apply the Dokka plugin for it, or disable individual project tasks using the Gradle API .
"""

@Suppress("DEPRECATION")
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractDokkaParentTask : AbstractDokkaTask() {

    /**
     * Dependencies from parent session (e.g. [DokkaMultiModuleTask] or [DokkaCollectorTask]) are stored
     * in the common container [dependsOn] from gradle [Task].
     * The [dependsOn] is a set and is available for everyone to write and has a type Object.
     * We will live in our own subset of objects with a type [AbstractDokkaTask].
     */
    @get:Nested
    internal val childDokkaTasks: Set<AbstractDokkaTask>
        get() = dependsOn
            .filterIsInstance<AbstractDokkaTask>()
            .toSet()

    /* By task reference */
    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addChildTask(task: AbstractDokkaTask) {
        dependsOn.add(task)
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTask(task: AbstractDokkaTask) {
        setDependsOn(dependsOn.filter { it != task })
    }

    /* By path */
    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addChildTask(path: String) {
        // Skip the adding process if a task is not found:
        // null will lead to the NPE failure during the dependency resolve process.
        // case: dokka is not applied to some submodules
        val task = project.tasks.findByPath(path) ?: return
        addChildTask(task.asAbstractDokkaTask())
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTask(path: String) {
        setDependsOn(dependsOn.filterNot {
            it is AbstractDokkaTask && it.path == project.absoluteProjectPath(path)
        })
    }

    /* By project reference and name */
    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            addChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            removeChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun addSubprojectChildTasks(childTasksName: String) {
        addChildTasks(project.subprojects, childTasksName)
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeSubprojectChildTasks(childTasksName: String) {
        removeChildTasks(project.subprojects, childTasksName)
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTasks(project: Project) {
        setDependsOn(dependsOn.filterNot {
            it is AbstractDokkaTask && parsePath(it.path).parent == parsePath(project.path)
        })
    }

    @Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
    fun removeChildTasks(projects: Iterable<Project>) {
        projects.forEach { project -> removeChildTasks(project) }
    }

    private fun Task.asAbstractDokkaTask(): AbstractDokkaTask {
        if (this is AbstractDokkaTask) return this

        throw IllegalArgumentException(
            "Only tasks of type ${AbstractDokkaTask::class.java.name} can be added as child for " +
                    "${AbstractDokkaParentTask::class.java.name} tasks.\n" +
                    "Found task ${this.path} of type ${this::class.java.name} added to ${this@AbstractDokkaParentTask.path}"
        )
    }
}

