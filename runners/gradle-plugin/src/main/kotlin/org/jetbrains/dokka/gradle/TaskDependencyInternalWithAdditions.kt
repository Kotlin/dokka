package org.jetbrains.dokka.gradle

import org.gradle.api.Task
import org.gradle.api.internal.tasks.AbstractTaskDependency
import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.gradle.api.internal.tasks.TaskDependencyResolveContext

operator fun TaskDependencyInternal.plus(tasks: Iterable<Task>): TaskDependencyInternal {
    return TaskDependencyInternalWithAdditions(this, tasks.toSet())
}

private class TaskDependencyInternalWithAdditions(
    private val dependency: TaskDependencyInternal,
    private val additionalTaskDependencies: Set<Task>
) : AbstractTaskDependency() {
    override fun visitDependencies(context: TaskDependencyResolveContext) {
        dependency.visitDependencies(context)
        additionalTaskDependencies.forEach(context::add)
    }
}
