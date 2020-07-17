package org.jetbrains

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
