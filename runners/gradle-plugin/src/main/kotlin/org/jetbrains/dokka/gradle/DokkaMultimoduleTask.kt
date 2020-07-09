package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaVersion
import org.jetbrains.dokka.plugability.Configurable
import java.net.URLClassLoader
import java.util.function.BiConsumer

open class DokkaMultimoduleTask : DefaultTask(), Configurable {

    @Input
    var documentationFileName: String = "README.md"

    @Input
    var outputFormat: String = "html"

    @Input
    var outputDirectory: String = ""

    @Classpath
    val runtime = project.configurations.create("${name}Runtime").apply {
        defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create("org.jetbrains.dokka:dokka-core:${DokkaVersion.version}"))
        }
    }

    @Classpath
    val plugins = project.configurations.create("${name}Plugin").apply {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
        isCanBeConsumed = false
    }

    @Input
    override val pluginsConfiguration: Map<String, String> = mutableMapOf()

    @TaskAction
    fun dokkaMultiplatform() {
        val kotlinColorsEnabledBefore = System.getProperty(DokkaTask.COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(DokkaTask.COLORS_ENABLED_PROPERTY, "false")

        try {
            val bootstrap = DokkaBootstrap(runtime, "org.jetbrains.dokka.DokkaMultimoduleBootstrapImpl")
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
        } finally {
            System.setProperty(DokkaTask.COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
    }

    @Internal
    internal fun getConfiguration(): GradleDokkaConfigurationImpl =
        GradleDokkaConfigurationImpl().apply {
            outputDir = project.file(outputDirectory).absolutePath
            format = outputFormat
            pluginsClasspath = plugins.resolve().toList()
            pluginsConfiguration = this@DokkaMultimoduleTask.pluginsConfiguration
            modules = project.subprojects
                .mapNotNull { subproject ->
                    subproject.tasks.withType(DokkaTask::class.java).firstOrNull()?.let { dokkaTask ->
                        GradleDokkaModuleDescription().apply {
                            name = subproject.name
                            path = subproject.projectDir.resolve(dokkaTask.outputDirectory)
                                .toRelativeString(project.file(outputDirectory))
                            docFile = subproject.projectDir.resolve(documentationFileName).absolutePath
                        }
                    }
                }
        }
}
