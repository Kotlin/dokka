/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.*
import org.jetbrains.dokka.gradle.dependencies.DependencyContainerNames

/**
 * Add some utilities to make migrating from V1 to V2 easier.
 *
 * - Create the same tasks as V1, but disable them and update the description.
 * - Create the same Configurations as V1, if they are missing from V2, and mark them as deprecated.
 */
internal fun addV2MigrationHelpers(
    project: Project
) {
    // Note: `dokkaPlugin` exists in the v2 plugin, so no migration helper is required.
    project.configurations.createDokkaDefaultRuntimeConfiguration()

    setupDokkaTasks(project, "GFM")
    setupDokkaTasks(project, "Javadoc", createDokkaPluginFormatConfiguration = false, multiModuleTaskSupported = false)
    setupDokkaTasks(project, "Jekyll")
    setupDokkaTasks(project, "HTML", createDokkaPluginFormatConfiguration = false)

    configureDokkaTaskConventions(project)
}

private fun configureDokkaTaskConventions(project: Project) {
    project.tasks.withType<@Suppress("DEPRECATION") AbstractDokkaTask>().configureEach task@{
        // The DGPv1 tasks are only present to prevent buildscripts with references to them from breaking.
        // The tasks are non-operable and should be hidden, to help nudge users to the DGPv2 tasks.
        // Setting tasks with group null will hide it when running `gradle tasks`,
        // and put it in the 'other' group in IntelliJ (which effectively hides it).
        @Suppress("UsePropertyAccessSyntax") // property-access syntax doesn't accept `null`
        setGroup(null)

        notCompatibleWithConfigurationCache("Dokka V1 tasks use deprecated Gradle features. Please migrate to Dokka Plugin V2, which fully supports Configuration Cache. See https://kotl.in/dokka-gradle-migration")

        // must have an output directory, else the doFirst won't run
        outputDirectory.set(temporaryDir)

        doFirst("Disable Dokka V1 task") {
            throw DokkaV1TaskDisabledException(
                buildString {
                    appendLine("Cannot run Dokka V1 tasks when V2 mode is enabled.")
                    appendLine("Dokka Gradle plugin V1 mode is deprecated, and scheduled to be removed in Dokka v2.2.0.")
                    appendLine("To finish migrating to V2 mode, please check the migration guide https://kotl.in/dokka-gradle-migration")
                    when {
                        "html" in this@task.name.lowercase() ->
                            appendLine("Suggestion: Use `dokkaGenerate` or `dokkaGenerateHtml` tasks instead.")

                        "javadoc" in this@task.name.lowercase() ->
                            appendLine("Suggestion: Use `dokkaGenerate` or `dokkaGenerateJavadoc` tasks instead.")

                        else -> {
                            // Don't suggest alternative tasks for GFM and Jekyll, since DGPv2 does not support these formats
                        }
                    }
                }
            )
        }
    }
}

internal class DokkaV1TaskDisabledException(
    message: String
) : UnsupportedOperationException(message)

/**
 * Creates dummy tasks and configurations for the given name and configuration, to help with migration.
 *
 * @see org.jetbrains.dokka.gradle.DokkaClassicPlugin.setupDokkaTasks
 */
private fun setupDokkaTasks(
    project: Project,
    format: String,
    multiModuleTaskSupported: Boolean = true,
    createDokkaPluginFormatConfiguration: Boolean = true,
) {
    val prettyFormat: String = format.lowercase().uppercaseFirstChar()
    val baseTaskName = "dokka$prettyFormat"
    val taskDesc = "[⚠ V1 tasks disabled]"

    val newConfs = DependencyContainerNames(format.lowercase())

    /** @see org.jetbrains.dokka.gradle.maybeCreateDokkaPluginConfiguration */
    fun ConfigurationContainer.createDokkaPluginConfiguration(taskName: String) {
        create("${taskName}Plugin") {
            declarable()
            deprecate(replaceWith = newConfs.pluginsClasspath)
        }
    }

    /** @see org.jetbrains.dokka.gradle.maybeCreateDokkaRuntimeConfiguration */
    fun ConfigurationContainer.createDokkaRuntimeConfiguration(taskName: String): Configuration {
        return create("${taskName}Runtime") {
            declarable()
            deprecate(replaceWith = newConfs.generatorClasspath)
        }
    }

    if (createDokkaPluginFormatConfiguration) {
        // Don't create dokka${Format}Plugin Configurations, v2 will create Configurations with the same name and purpose.
        project.configurations.createDokkaPluginConfiguration(taskName = baseTaskName)
        project.configurations.createDokkaRuntimeConfiguration(taskName = baseTaskName)
    }

    project.tasks.register<@Suppress("DEPRECATION") DokkaTask>(baseTaskName) {
        description = "$taskDesc Generates documentation in '$format' format."
    }

    if (project.parent != null) {
        val partialName = "${baseTaskName}Partial"
        project.configurations.createDokkaPluginConfiguration(taskName = partialName)
        project.configurations.createDokkaRuntimeConfiguration(taskName = partialName)
        project.tasks.register<@Suppress("DEPRECATION") DokkaTaskPartial>(partialName) {
            description = "$taskDesc Generates documentation in '$format' format."
        }
    }

    if (project.subprojects.isNotEmpty()) {
        if (multiModuleTaskSupported) {
            val multiModuleName = "${baseTaskName}MultiModule"
            project.configurations.createDokkaPluginConfiguration(taskName = multiModuleName)
            project.configurations.createDokkaRuntimeConfiguration(taskName = multiModuleName)

            project.tasks.register<@Suppress("DEPRECATION") DokkaMultiModuleTask>(multiModuleName) {
                description =
                    "$taskDesc Runs all subprojects '$name' tasks and generates $format module navigation page."
            }
        }

        project.tasks.register<@Suppress("DEPRECATION") DokkaCollectorTask>("${baseTaskName}Collector") {
            description =
                "$taskDesc Generates documentation merging all subprojects '$baseTaskName' tasks into one virtual module."
        }
    }
}

/** @see org.jetbrains.dokka.gradle.maybeCreateDokkaDefaultRuntimeConfiguration */
private fun ConfigurationContainer.createDokkaDefaultRuntimeConfiguration(): Configuration {
    return create("dokkaRuntime") {
        description = "[⚠ V1 Configurations are disabled] Classpath used to execute the Dokka Generator."
        declarable()
        deprecate(DependencyContainerNames("").generatorClasspath)
    }
}


/**
 * Use an internal Gradle feature to mark [Configuration]s as deprecated.
 */
private fun Configuration.deprecate(replaceWith: String) {
    try {
        if (this is DeprecatableConfiguration) {
            addDeclarationAlternatives(replaceWith)
        }
    } catch (_: Throwable) {
        // Deprecating configurations is an internal Gradle feature, so it might be unstable.
        // Because these migration helpers are temporary, just ignore all errors.
    }
}
