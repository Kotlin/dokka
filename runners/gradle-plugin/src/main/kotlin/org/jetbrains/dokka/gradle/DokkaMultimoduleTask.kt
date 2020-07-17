package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.plugability.Configurable

open class DokkaMultimoduleTask : AbstractDokkaTask(), Configurable {

    @Input
    var documentationFileName: String = "README.md"


    @Input
    var dokkaTaskNames: Set<String> = setOf()
        set(value) {
            field = value.toSet()
            setDependsOn(getSubprojectDokkaTasks(value))
        }


    override fun generate() {
        val bootstrap = DokkaBootstrap("org.jetbrains.dokka.DokkaMultimoduleBootstrapImpl")
        val gson = GsonBuilder().setPrettyPrinting().create()
        val configuration = getConfiguration()
        bootstrap.configure(gson.toJson(configuration)) { level, message ->
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

    @Internal
    internal fun getConfiguration(): GradleDokkaConfigurationImpl =
        GradleDokkaConfigurationImpl().apply {
            outputDir = project.file(outputDirectory).absolutePath
            pluginsClasspath = plugins.resolve().toList()
            pluginsConfiguration = this@DokkaMultimoduleTask.pluginsConfiguration
            modules = getSubprojectDokkaTasks(dokkaTaskNames).map { dokkaTask ->
                GradleDokkaModuleDescription().apply {
                    name = dokkaTask.project.name
                    path = dokkaTask.project.projectDir.resolve(dokkaTask.outputDirectory)
                        .toRelativeString(project.file(outputDirectory))
                    docFile = dokkaTask.project.projectDir.resolve(documentationFileName).absolutePath
                }
            }
        }

    private fun getSubprojectDokkaTasks(dokkaTaskName: String): List<DokkaTask> {
        return project.subprojects
            .mapNotNull { subproject -> subproject.tasks.findByName(dokkaTaskName) as? DokkaTask }
    }

    private fun getSubprojectDokkaTasks(dokkaTaskNames: Set<String>): List<DokkaTask> {
        return dokkaTaskNames.flatMap { dokkaTaskName -> getSubprojectDokkaTasks(dokkaTaskName) }
    }

    init {
        group = DOCUMENTATION_GROUP
    }
}
