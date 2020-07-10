package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.plugability.Configurable


abstract class AbstractDokkaTask : DefaultTask(), Configurable {
    @Input
    var outputDirectory: String = defaultDokkaOutputDirectory().absolutePath

    @Suppress("unused")
    @Deprecated("This setting is a noop and will be removed")
    @get:Internal
    var outputFormat: String = ""

    @Input
    override val pluginsConfiguration: Map<String, String> = mutableMapOf()

    @Classpath
    val plugins: Configuration = project.maybeCreateDokkaPluginConfiguration(name)

    @Classpath
    val runtime: Configuration = project.maybeCreateDokkaRuntimeConfiguration(name)

    @TaskAction
    protected fun run() {
        val kotlinColorsEnabledBefore = System.getProperty(DokkaTask.COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(DokkaTask.COLORS_ENABLED_PROPERTY, "false")
        try {
            generate()
        } finally {
            System.setProperty(DokkaTask.COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
    }

    protected abstract fun generate()

    protected fun DokkaBootstrap(bootstrapClassFQName: String): DokkaBootstrap {
        return DokkaBootstrap(runtime, bootstrapClassFQName)
    }
}
