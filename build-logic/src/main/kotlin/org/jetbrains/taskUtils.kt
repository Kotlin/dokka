package org.jetbrains

import org.gradle.api.Project
import org.gradle.api.Task

fun Task.dependsOnMavenLocalPublication() {
    project.rootProject.allprojects.forEach { otherProject ->
        otherProject.invokeWhenEvaluated { evaluatedProject ->
            evaluatedProject.tasks.findByName("publishToMavenLocal")?.let { publishingTask ->
                this.dependsOn(publishingTask)
            }
        }
    }
}

val Project.isLocalPublication: Boolean
    get() = gradle.startParameter.taskNames.any {
        it.endsWith("publishToMavenLocal", ignoreCase = true) ||
                it.endsWith("integrationTest", ignoreCase = true) ||
                it.endsWith("check", ignoreCase = true) ||
                it.endsWith("test", ignoreCase = true)
    }
