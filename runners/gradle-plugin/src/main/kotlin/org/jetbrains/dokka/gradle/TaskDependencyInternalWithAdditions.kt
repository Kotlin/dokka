package org.jetbrains.dokka.gradle

import org.gradle.api.Task
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.internal.tasks.TaskDependencyInternal

internal operator fun TaskDependencyInternal.plus(tasks: Iterable<Task>): TaskDependencyInternal =
    TaskDependencyInternalWithAdditions(this, tasks.toSet())

private class TaskDependencyInternalWithAdditions(
    dependency: TaskDependencyInternal,
    additionalTaskDependencies: Set<Task>,
) : DefaultTaskDependency() {

    init {
        add(dependency, additionalTaskDependencies)
    }
}
