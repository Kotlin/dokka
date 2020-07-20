package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.toJsonString

open class DokkaCollectorTask : AbstractDokkaTask() {

    @Input
    var modules: List<String> = emptyList()

    @Input
    var dokkaTaskNames: Set<String> = setOf()

    override fun generate() {
        val configurations = getSubprojectDokkaTasks(dokkaTaskNames)
            .mapNotNull { dokkaTask -> dokkaTask.getConfigurationOrNull() }

        val initial = GradleDokkaConfigurationImpl().apply {
            outputDir = outputDirectory
            cacheRoot = configurations.first().cacheRoot
        }

        val configuration = configurations.fold(initial) { acc, it: GradleDokkaConfigurationImpl ->
            if (acc.cacheRoot != it.cacheRoot)
                throw IllegalStateException("Dokka task configurations differ on core argument cacheRoot")
            acc.sourceSets = acc.sourceSets + it.sourceSets
            acc.pluginsClasspath = (acc.pluginsClasspath + it.pluginsClasspath).distinct()
            acc
        }

        val bootstrap = DokkaBootstrap("org.jetbrains.dokka.DokkaBootstrapImpl")
        bootstrap.configure(configuration.toJsonString()) { level, message ->
            when (level) {
                "debug" -> logger.debug(message)
                "info" -> logger.info(message)
                "progress" -> logger.lifecycle(message)
                "warn" -> logger.warn(message)
                "error" -> logger.error(message)
            }
        }
        bootstrap.generate()
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
