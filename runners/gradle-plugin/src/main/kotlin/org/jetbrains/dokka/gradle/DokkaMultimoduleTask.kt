package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.plugability.Configurable

open class DokkaMultimoduleTask : AbstractDokkaTask(), Configurable {

    @Input
    var documentationFileName: String = "README.md"


    @Input
    val dokkaTaskNames: MutableSet<String> = mutableSetOf()

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
            modules = project.subprojects
                .flatMap { subProject -> dokkaTaskNames.mapNotNull(subProject.tasks::findByName) }
                .filterIsInstance<DokkaTask>()
                .map { dokkaTask ->
                    GradleDokkaModuleDescription().apply {
                        name = dokkaTask.project.name
                        path = dokkaTask.project.projectDir.resolve(dokkaTask.outputDirectory)
                            .toRelativeString(project.file(outputDirectory))
                        docFile = dokkaTask.project.projectDir.resolve(documentationFileName).absolutePath
                    }
                }
        }
}
