package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.lang.IllegalStateException

open class DokkaCollectorTask : DefaultTask() {

    @Input
    var modules: List<String> = emptyList()

    @Input
    var outputDirectory: String = ""

    private lateinit var configuration: GradleDokkaConfigurationImpl

    @TaskAction
    fun collect() {
        val sourceSets = getProjects(project).filter { it.name in modules }.flatMap {
            val tasks = try {
                it.tasks.withType(DokkaTask::class.java)
            } catch (e: UnknownTaskException) {
                throw IllegalStateException("No dokka task declared in module ${it.name}")
            }
            tasks.map { it.getConfigurationOrNull() }
        }.filterNotNull()

        val initial = GradleDokkaConfigurationImpl().apply {
            outputDir = outputDirectory
            cacheRoot = sourceSets.first().cacheRoot
            format = sourceSets.first().format
        }

        configuration = sourceSets.fold(initial) { acc, it: GradleDokkaConfigurationImpl ->
            if(acc.format != it.format || acc.cacheRoot != it.cacheRoot)
                throw IllegalStateException("Dokka task configurations differ on core arguments (format, cacheRoot)")
            acc.sourceSets = acc.sourceSets + it.sourceSets
            acc.pluginsClasspath = (acc.pluginsClasspath + it.pluginsClasspath).distinct()
            acc
        }
        project.tasks.withType(DokkaTask::class.java).configureEach { it.config = configuration }
    }

    init {
        finalizedBy(project.tasks.getByName(DOKKA_TASK_NAME))
    }

    private fun getProjects(project: Project): Set<Project> =
        project.subprojects + project.subprojects.flatMap { getProjects(it) }

}
