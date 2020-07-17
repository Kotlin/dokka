package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskDependency
import java.lang.IllegalStateException

open class DokkaCollectorTask : DefaultTask() {

    @Input
    var modules: List<String> = emptyList()

    @Input
    var outputDirectory: String = defaultDokkaOutputDirectory().absolutePath

    @Input
    var dokkaTaskNames: Set<String> = setOf()

    override fun getFinalizedBy(): TaskDependency {
        val dokkaTasks = getSubprojectDokkaTasks(dokkaTaskNames)
        dokkaTasks.forEach { dokkaTask -> finalizedBy(dokkaTask) }
        dokkaTasks.zipWithNext().forEach { (first, second) -> first.mustRunAfter(second) }
        return super.getFinalizedBy()
    }

    @TaskAction
    fun collect() {
        val configurations = getSubprojectDokkaTasks(dokkaTaskNames)
            .mapNotNull { dokkaTask -> dokkaTask.getConfigurationOrNull() }

        val initial = GradleDokkaConfigurationImpl().apply {
            outputDir = outputDirectory
            cacheRoot = configurations.first().cacheRoot
        }

        // TODO this certainly not the ideal solution
        val configuration = configurations.fold(initial) { acc, it: GradleDokkaConfigurationImpl ->
            if (acc.cacheRoot != it.cacheRoot)
                throw IllegalStateException("Dokka task configurations differ on core argument cacheRoot")
            acc.sourceSets = acc.sourceSets + it.sourceSets
            acc.pluginsClasspath = (acc.pluginsClasspath + it.pluginsClasspath).distinct()
            acc
        }
        getSubprojectDokkaTasks(dokkaTaskNames).forEach { it.enforcedConfiguration = configuration }
    }

    private fun getSubprojectDokkaTasks(dokkaTaskName: String): List<DokkaTask> {
        return project.subprojects
            .filter { subproject -> subproject.name in modules }
            .mapNotNull { subproject -> subproject.tasks.findByName(dokkaTaskName) as? DokkaTask }
    }

    private fun getSubprojectDokkaTasks(dokkaTaskNames: Set<String>): List<DokkaTask> {
        return dokkaTaskNames.flatMap { dokkaTaskName -> getSubprojectDokkaTasks(dokkaTaskName) }
    }

    init {
        group = DOCUMENTATION_GROUP
    }
}
