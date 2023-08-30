/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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

