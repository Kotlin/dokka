@file:Suppress("UnstableApiUsage")

package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.mapProperty
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.toJsonString
import java.io.File
import java.util.function.BiConsumer
import kotlin.reflect.KClass

abstract class AbstractDokkaTask(
    private val bootstrapClass: KClass<out DokkaBootstrap> = DokkaBootstrap::class
) : DefaultTask() {

    @Input
    val moduleName: Property<String> = project.objects.safeProperty<String>()
        .safeConvention(project.name)

    @Input
    val moduleVersion: Property<String> = project.objects.safeProperty<String>()
        .safeConvention(project.version.toString())

    @OutputDirectory
    val outputDirectory: Property<File> = project.objects.safeProperty<File>()
        .safeConvention(defaultDokkaOutputDirectory())

    @Optional
    @InputDirectory
    val cacheRoot: Property<File?> = project.objects.safeProperty()

    @Input
    val failOnWarning: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.failOnWarning)

    @Input
    val offlineMode: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.offlineMode)

    @Input
    val pluginsConfiguration: MapProperty<String, String> = project.objects.mapProperty()

    @Classpath
    val plugins: Configuration = project.maybeCreateDokkaPluginConfiguration(name)

    @Classpath
    val runtime: Configuration = project.maybeCreateDokkaRuntimeConfiguration(name)

    final override fun doFirst(action: Action<in Task>): Task = super.doFirst(action)

    final override fun doFirst(action: Closure<*>): Task = super.doFirst(action)

    @TaskAction
    internal open fun generateDocumentation() {
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
