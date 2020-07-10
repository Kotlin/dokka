package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.plugability.Configurable

abstract class AbstractDokkaTask : DefaultTask(), Configurable {
    @Input
    var outputDirectory: String = defaultDokkaOutputDirectory().absolutePath

    @Input
    override val pluginsConfiguration: Map<String, String> = mutableMapOf()

    @Classpath
    val plugins: Configuration = project.configurations.create("${name}Plugin").apply {
        defaultDependencies { dependencies ->
            dependencies.add(project.dokkaArtifacts.dokkaBase)
        }
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
        isCanBeConsumed = false
    }

    @Classpath
    val runtime = project.configurations.create("${name}Runtime").apply {
        defaultDependencies { dependencies ->
            dependencies.add(project.dokkaArtifacts.dokkaCore)
        }
    }

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
