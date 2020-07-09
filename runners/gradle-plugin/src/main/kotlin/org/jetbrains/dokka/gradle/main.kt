package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.DokkaVersion
import java.io.File

internal const val DOKKA_TASK_NAME = "dokka"
internal const val DOKKA_COLLECTOR_TASK_NAME = "dokkaCollector"
internal const val DOKKA_MULTIMODULE_TASK_NAME = "dokkaMultimodule"

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        addDokkaTasks(project)
        addDokkaCollectorTasks(project)
        addDokkaMultimoduleTasks(project.rootProject)
    }

    private fun addDokkaTasks(project: Project) {
        project.tasks.register<DokkaTask>(DOKKA_TASK_NAME) {
            val dokkaBase = project.dependencies.create("org.jetbrains.dokka:dokka-base:${DokkaVersion.version}")
            plugins.dependencies.add(dokkaBase)
            outputDirectory = File(project.buildDir, DOKKA_TASK_NAME).absolutePath
        }
    }

    private fun addDokkaCollectorTasks(project: Project) {
        project.tasks.register<DokkaCollectorTask>(DOKKA_COLLECTOR_TASK_NAME) {
            outputDirectory = File(project.buildDir, DOKKA_TASK_NAME).absolutePath
        }
    }

    private fun addDokkaMultimoduleTasks(project: Project) {
        project.tasks.register<DokkaMultimoduleTask>(DOKKA_MULTIMODULE_TASK_NAME) {
            outputDirectory = File(project.buildDir, DOKKA_TASK_NAME).absolutePath
        }
    }
}
