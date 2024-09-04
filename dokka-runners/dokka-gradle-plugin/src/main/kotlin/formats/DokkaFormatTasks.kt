/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.dependencies.FormatDependenciesManager
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.configuring
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateModuleTask
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.dokka.gradle.tasks.TaskNames

/** Tasks for generating a [DokkaPublication] in a specific format. */
@DokkaInternalApi
class DokkaFormatTasks(
    project: Project,
    private val publication: DokkaPublication,
    private val dokkaExtension: DokkaExtension,
    private val formatDependencies: FormatDependenciesManager,

    private val providers: ProviderFactory,
) {
    private val formatName: String get() = publication.formatName

    private val taskNames = TaskNames(formatName)

    private fun DokkaGenerateTask.applyFormatSpecificConfiguration() {
        runtimeClasspath.from(
            formatDependencies.dokkaGeneratorClasspathResolver
        )
        generator.apply {
            publicationEnabled.convention(publication.enabled)

            failOnWarning.convention(publication.failOnWarning)
            finalizeCoroutines.convention(publication.finalizeCoroutines)
            includes.from(publication.includes)
            moduleName.convention(publication.moduleName)
            moduleVersion.convention(publication.moduleVersion)
            offlineMode.convention(publication.offlineMode)
            pluginsConfiguration.addAllLater(providers.provider { publication.pluginsConfiguration })
            pluginsClasspath.from(
                formatDependencies.dokkaPluginsIntransitiveClasspathResolver
            )
            suppressInheritedMembers.convention(publication.suppressInheritedMembers)
            suppressObviousFunctions.convention(publication.suppressObviousFunctions)
        }
    }

    val generatePublication: TaskProvider<DokkaGeneratePublicationTask> =
        project.tasks.register<DokkaGeneratePublicationTask>(
            taskNames.generatePublication,
            publication.pluginsConfiguration,
        ).configuring {
            description = "Executes the Dokka Generator, generating the $formatName publication"

            outputDirectory.convention(dokkaExtension.dokkaPublicationDirectory.dir(formatName))

            applyFormatSpecificConfiguration()
        }

    val generateModule: TaskProvider<DokkaGenerateModuleTask> =
        project.tasks.register<DokkaGenerateModuleTask>(
            taskNames.generateModule,
            publication.pluginsConfiguration,
        ).configuring {
            description = "Executes the Dokka Generator, generating a $formatName module"

            outputDirectory.convention(dokkaExtension.dokkaModuleDirectory.dir(formatName))

            applyFormatSpecificConfiguration()
        }
}
