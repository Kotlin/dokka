package org.jetbrains.dokka.gradle

import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.distibutions.DokkaPluginConfigurations
import org.jetbrains.dokka.gradle.distibutions.setupDokkaConfigurations
import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.dokka.gradle.tasks.DokkaModuleConfigurationTask
import javax.inject.Inject


abstract class DokkaPlugin @Inject constructor(
    private val provider: ProviderFactory,
    private val layout: ProjectLayout,
) : Plugin<Project> {

    override fun apply(target: Project) {

        val dokkaSettings = target.extensions.create<DokkaPluginSettings>(EXTENSION_NAME).apply {
            dokkaVersion.convention("1.7.20")
            dokkaWorkDir.convention(target.rootProject.layout.buildDirectory.file("dokka-work-dir"))
        }

        val dokkaConfigurations = target.setupDokkaConfigurations(dokkaSettings)

        val dokkaModuleConfigurationTask = target.tasks.registerDokkaModuleConfigurationTask()

        dokkaConfigurations.dokkaModuleDescriptorsElements.configure {
            outgoing {
                artifact(dokkaModuleConfigurationTask.map { it.dokkaModuleConfigurationJson })
            }
        }

        val dokkaConfigurationTask = target.tasks.registerCreateDokkaConfigurationTask(dokkaConfigurations)

        target.tasks.registerDokkaExecutorTask(dokkaConfigurationTask)

        target.tasks.withType<DokkaConfigurationTask>().configureEach {
            cacheRoot.convention(target.rootProject.layout.buildDirectory.dir("dokka-cache"))
            delayTemplateSubstitution.convention(true)
            dokkaConfigurationJson.convention(layout.buildDirectory.file("dokka-config/dokka_configuration.json"))
            outputDir.convention(layout.buildDirectory.dir("dokka-output"))
            failOnWarning.convention(false)
            finalizeCoroutines.convention(false)
            suppressInheritedMembers.convention(false)
            suppressObviousFunctions.convention(false)
            offlineMode.convention(false)
            moduleName.convention(provider.provider { project.name })
            moduleVersion.convention(provider.provider { project.version.toString() })
        }
    }


    private fun TaskContainer.registerDokkaExecutorTask(
        dokkaConfigurationTask: TaskProvider<DokkaConfigurationTask>,
    ): TaskProvider<DokkaGenerateTask> {
        return register<DokkaGenerateTask>("dokkaGenerate") {
            dokkaConfigurationJson.set(dokkaConfigurationTask.flatMap { it.dokkaConfigurationJson })
            cacheDirectory.convention(
                layout.dir(dokkaConfigurationValue { cacheRoot })
            )
            outputDirectory.convention(
                layout.dir(dokkaConfigurationValue { outputDir })
            )
        }
    }


    private fun TaskContainer.registerCreateDokkaConfigurationTask(
        dokkaConfigurations: DokkaPluginConfigurations,
    ): TaskProvider<DokkaConfigurationTask> {
        return register<DokkaConfigurationTask>("createDokkaConfiguration") {
            dokkaModuleDescriptorFiles.from(
                dokkaConfigurations.dokkaModuleDescriptorsConsumer.map { elements ->
                    elements.incoming.artifactView { lenient(true) }.files
                }
            )
        }
    }


    private fun TaskContainer.registerDokkaModuleConfigurationTask(): TaskProvider<DokkaModuleConfigurationTask> {
        return register<DokkaModuleConfigurationTask>("createDokkaModuleConfiguration") {
            moduleName.set(name.map { it.takeIf(Char::isLetterOrDigit) ?: "-" }.joinToString(""))
            dokkaModuleConfigurationJson.set(
                moduleName.flatMap { moduleName ->
                    layout.buildDirectory.file("dokka/$moduleName.json")
                }
            )
            moduleOutputDirectoryPath(layout.buildDirectory.dir("dokka/module-output"))
            sourceOutputDirectory(layout.buildDirectory.dir("dokka/source-output"))
        }
    }


    companion object {

        const val EXTENSION_NAME = "dokka"
        const val CONFIGURATION_NAME__DOKKA = "dokka"

        /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration] from projects */
        const val CONFIGURATION_NAME__DOKKA_CONFIGURATIONS = "dokkaConfigurations"

        /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
        const val CONFIGURATION_NAME__DOKKA_CONFIGURATION_ELEMENTS = "dokkaConfigurationElements"

        /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] from projects */
        const val CONFIGURATION_NAME__DOKKA_MODULE_DESCRIPTORS = "dokkaModuleDescriptor"

        /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] to other projects */
        const val CONFIGURATION_NAME__DOKKA_MODULE_DESCRIPTOR_ELEMENTS = "dokkaModuleDescriptorElements"

        const val CONFIGURATION_NAME__DOKKA_RUNTIME_CLASSPATH = "dokkaRuntimeClasspath"

        const val TASK_GROUP = "dokka"
        const val TASK_NAME__CREATE_DOKKA_CONFIGURATION = "createDokkaConfiguration"
        const val TASK_NAME__CREATE_DOKKA_MODULE_DESCRIPTOR = "createDokkaModuleDescriptor"

        const val DOKKA__SERVICE_NAME__DOCUMENTATION_GENERATOR = "dokkaDocumentationGenerator"

        internal val jsonMapper = Json {
            prettyPrint = true
        }

    }
}
