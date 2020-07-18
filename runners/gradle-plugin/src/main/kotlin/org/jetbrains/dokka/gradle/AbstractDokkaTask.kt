package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.plugability.Configurable
import org.jetbrains.dokka.toJsonString
import java.io.File
import java.util.function.BiConsumer
import kotlin.reflect.KClass

abstract class AbstractDokkaTask(
    private val bootstrapClass: KClass<out DokkaBootstrap> = DokkaBootstrap::class
) : DefaultTask(), Configurable {

    @OutputDirectory
    var outputDirectory: File = defaultDokkaOutputDirectory()

    @Optional
    @InputDirectory
    var cacheRoot: File? = null

    @Input
    var failOnWarning: Boolean = false

    @Input
    var offlineMode: Boolean = false

    @Input
    override val pluginsConfiguration: MutableMap<String, String> = mutableMapOf()

    @Classpath
    val plugins: Configuration = project.maybeCreateDokkaPluginConfiguration(name)

    @Classpath
    val runtime: Configuration = project.maybeCreateDokkaRuntimeConfiguration(name)

    @TaskAction
    protected open fun generateDocumentation() {
        DokkaBootstrap(runtime, bootstrapClass).apply {
            configure(buildDokkaConfiguration().toJsonString(), createProxyLogger())
            generate()
        }
    }

    internal abstract fun buildDokkaConfiguration(): DokkaConfigurationImpl

    private fun createProxyLogger(): BiConsumer<String, String> = BiConsumer { level, message ->
        when (level) {
            "debug" -> logger.debug(message)
            "info" -> logger.info(message)
            "progress" -> logger.lifecycle(message)
            "warn" -> logger.warn(message)
            "error" -> logger.error(message)
        }
    }

    init {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
    }
}
