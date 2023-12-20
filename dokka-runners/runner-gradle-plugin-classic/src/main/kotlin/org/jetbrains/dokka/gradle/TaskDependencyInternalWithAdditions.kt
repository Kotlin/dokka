/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
