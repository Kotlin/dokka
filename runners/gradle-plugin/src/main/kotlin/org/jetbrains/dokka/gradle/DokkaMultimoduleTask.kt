package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.dokka.DokkaBootstrap
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
    lateinit var pluginsConfig: Configuration

    var dokkaRuntime: Configuration? = null

    override val pluginsConfiguration: Map<String, String> = mutableMapOf()

    @TaskAction
    fun dokkaMultiplatform() {
        val kotlinColorsEnabledBefore = System.getProperty(DokkaTask.COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(DokkaTask.COLORS_ENABLED_PROPERTY, "false")

        try {
            loadFatJar()
            val bootstrapClass =
                ClassloaderContainer.fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaMultimoduleBootstrapImpl")
            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
            val bootstrapProxy: DokkaBootstrap = automagicTypedProxy(
                javaClass.classLoader,
                bootstrapInstance
            )
            val gson = GsonBuilder().setPrettyPrinting().create()
            val configuration = getConfiguration()
            bootstrapProxy.configure(
                BiConsumer { level, message ->
                    when (level) {
                        "debug" -> logger.debug(message)
                        "info" -> logger.info(message)
                        "progress" -> logger.lifecycle(message)
                        "warn" -> logger.warn(message)
                        "error" -> logger.error(message)
                    }
                },
                gson.toJson(configuration)
            )

            bootstrapProxy.generate()
        } finally {
            System.setProperty(DokkaTask.COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
    }

    internal fun getConfiguration(): GradleDokkaConfigurationImpl =
        GradleDokkaConfigurationImpl().apply {
            outputDir = project.file(outputDirectory).absolutePath
            format = outputFormat
            pluginsClasspath = pluginsConfig.resolve().toList()
            pluginsConfiguration = this@DokkaMultimoduleTask.pluginsConfiguration
            modules = project.subprojects
                .mapNotNull { subproject ->
                    subproject.tasks.withType(DokkaTask::class.java).firstOrNull()?.let { dokkaTask ->
                        GradleDokkaModuleDescription().apply {
                            name = subproject.name
                            path =
                                subproject.projectDir.resolve(dokkaTask.outputDirectory).toRelativeString(
                                    project.file(outputDirectory)
                                )
                            docFile = subproject.projectDir.resolve(documentationFileName).absolutePath
                        }
                    }
                }
        }

    private fun loadFatJar() {
        if (ClassloaderContainer.fatJarClassLoader == null) {
            val jars = dokkaRuntime!!.resolve()
            ClassloaderContainer.fatJarClassLoader = URLClassLoader(
                jars.map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getSystemClassLoader().parent
            )
        }
    }
}
