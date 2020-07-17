package org.jetbrains

import org.gradle.api.Project

fun Project.whenEvaluated(action: Project.() -> Unit) {
    if (state.executed) {
        action()
    } else {
        afterEvaluate { action() }
    }
}

fun Project.invokeWhenEvaluated(action: (project: Project) -> Unit) {
    whenEvaluated { action(this) }
}

