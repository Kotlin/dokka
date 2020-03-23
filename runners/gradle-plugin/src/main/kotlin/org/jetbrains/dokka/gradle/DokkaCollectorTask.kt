package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByName
import java.lang.IllegalStateException

open class DokkaCollectorTask : DefaultTask() {

    @Input
    var modules: List<String> = emptyList()

    @Input
    var outputDirectory: String = ""

    private lateinit var configuration: GradleDokkaConfigurationImpl

    @TaskAction
    fun collect() {
        val passesConfigurations = getProjects(project).filter { it.name in modules }.map {
            val task = try {
                it.tasks.getByName(DOKKA_TASK_NAME, DokkaTask::class)
            } catch (e: UnknownTaskException) {
                throw IllegalStateException("No dokka task declared in module ${it.name}")
            }
            task.getConfiguration()
        }

        val initial = GradleDokkaConfigurationImpl().apply {
            outputDir = outputDirectory
            cacheRoot = passesConfigurations.first().cacheRoot
            format = passesConfigurations.first().format
            generateIndexPages = passesConfigurations.first().generateIndexPages
        }

        configuration = passesConfigurations.fold(initial) { acc, it: GradleDokkaConfigurationImpl ->
            if(acc.format != it.format || acc.generateIndexPages != it.generateIndexPages || acc.cacheRoot != it.cacheRoot)
                throw IllegalStateException("Dokka task configurations differ on core arguments (format, generateIndexPages, cacheRoot)")
            acc.passesConfigurations = acc.passesConfigurations + it.passesConfigurations
            acc.pluginsClasspath = (acc.pluginsClasspath + it.pluginsClasspath).distinct()
            acc
        }
        project.tasks.getByName(DOKKA_TASK_NAME).setProperty("config", configuration)
    }

    init {
        finalizedBy(project.tasks.getByName(DOKKA_TASK_NAME))
    }

    private fun getProjects(project: Project): Set<Project> =
            project.subprojects + project.subprojects.flatMap { getProjects(it) }

}