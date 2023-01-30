package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Nested
import org.gradle.work.DisableCachingByDefault

/*
We store dependencies from parent session (e.g. multimodule or collector)
in the common container `dependsOn` from gradle `Task`.
The `dependsOn` is a set and is available for everyone to write and has a type `Object`.
We will live in our own subset of objects with a type `AbstractDokkaTask`.
 */
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractDokkaParentTask : AbstractDokkaTask() {

    @get:Nested
    internal val childDokkaTasks: Set<AbstractDokkaTask>
        get() = dependsOn
            .filterIsInstance<AbstractDokkaTask>()
            .toSet()

    /* By task reference */
    fun addChildTask(task: AbstractDokkaTask) {
        dependsOn.add(task)
    }

    fun removeChildTask(task: AbstractDokkaTask) {
        setDependsOn(dependsOn.filter { it != task })
    }

    /* By path */
    fun addChildTask(path: String) {
        // Skip the adding process if a task is not found:
        // null will lead to the NPE failure during the dependency resolve process.
        // case: dokka is not applied to some submodules
        val task = project.tasks.findByPath(path) ?: return
        addChildTask(task.asAbstractDokkaTask())
    }

    fun removeChildTask(path: String) {
        setDependsOn(dependsOn.filterNot {
            it is AbstractDokkaTask && it.path == project.absoluteProjectPath(path)
        })
    }

    /* By project reference and name */
    fun addChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            addChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    fun removeChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            removeChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    fun addSubprojectChildTasks(childTasksName: String) {
        addChildTasks(project.subprojects, childTasksName)
    }

    fun removeSubprojectChildTasks(childTasksName: String) {
        removeChildTasks(project.subprojects, childTasksName)
    }

    fun removeChildTasks(project: Project) {
        setDependsOn(dependsOn.filterNot {
            it is AbstractDokkaTask && parsePath(it.path).parent == parsePath(project.path)
        })
    }

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

