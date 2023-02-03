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
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.adapters.DokkaKotlinAdapter
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

        val dokkaConfigurationTask = target.tasks.registerCreateDokkaConfigurationTask(dokkaConfigurations)

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
//            pluginsConfiguration.add(
//                DokkaConfigurationKxs.PluginConfigurationKxs(
//                    fqPluginName = "org.jetbrains.dokka.base.DokkaBase",
//                    serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
//                    values = "{}",
//                )
//            )
        }

        val dokkaGenerateTask = target.tasks.registerDokkaGenerateTask(dokkaConfigurationTask, dokkaConfigurations)

        val dokkaModuleConfigurationTask = target.tasks.registerDokkaModuleConfigurationTask(dokkaGenerateTask)

        dokkaConfigurations.dokkaModuleDescriptorsElements.configure {
            outgoing {
                artifact(dokkaModuleConfigurationTask.map { it.dokkaModuleConfigurationJson })
            }
        }

        // apply the plugin that will autoconfigure Dokka to use the sources of a Kotlin project
        target.pluginManager.apply(DokkaKotlinAdapter::class)
    }


    private fun TaskContainer.registerDokkaGenerateTask(
        dokkaConfigurationTask: TaskProvider<DokkaConfigurationTask>,
        dokkaConfigurations: DokkaPluginConfigurations,
    ): TaskProvider<DokkaGenerateTask> {
        return register<DokkaGenerateTask>(TaskName.DOKKA_GENERATE) {
            dokkaConfigurationJson.set(dokkaConfigurationTask.flatMap { it.dokkaConfigurationJson })
            cacheDirectory.convention(dokkaConfigurationTask.flatMap { it.cacheRoot })
            outputDirectory.convention(dokkaConfigurationTask.flatMap { it.outputDir })
//            cacheDirectory.convention(
//                layout.dir(dokkaConfigurationValue { cacheRoot })
//            )
//            outputDirectory.convention(
//                layout.dir(dokkaConfigurationValue { outputDir })
//            )
            runtimeClasspath.from(dokkaConfigurations.dokkaRuntimeClasspath)
            pluginClasspath.from(dokkaConfigurations.dokkaPluginsClasspath)
        }
    }


    private fun TaskContainer.registerCreateDokkaConfigurationTask(
        dokkaConfigurations: DokkaPluginConfigurations,
    ): TaskProvider<DokkaConfigurationTask> {
        val dokkaConfigurationTask =  register<DokkaConfigurationTask>(TaskName.CREATE_DOKKA_CONFIGURATION)

        withType<DokkaConfigurationTask>().configureEach{

            // depend on Dokka Configurations from other subprojects
            dokkaSubprojectConfigurations.from(
                dokkaConfigurations.dokkaConfigurationsConsumer.map { elements ->
                    elements.incoming.artifactView { lenient(true) }.files
                }
            )

            // depend on Dokka Module Configurations from other subprojects
            dokkaModuleDescriptorFiles.from(
                dokkaConfigurations.dokkaModuleDescriptorsConsumer.map { elements ->
                    elements.incoming.artifactView { lenient(true) }.files
                }
            )

            //
            pluginsClasspath.from(
                dokkaConfigurations.dokkaPluginsClasspath.map { elements ->
                    elements.incoming.artifactView { lenient(true) }.files
                }
            )
        }

        dokkaConfigurations.dokkaConfigurationsElements.configure {
            outgoing {
                artifact(dokkaConfigurationTask.flatMap { it.dokkaConfigurationJson })
            }
        }

        return dokkaConfigurationTask
    }


    private fun TaskContainer.registerDokkaModuleConfigurationTask(
        dokkaGenerateTask: TaskProvider<DokkaGenerateTask>,
    ): TaskProvider<DokkaModuleConfigurationTask> {
        return register<DokkaModuleConfigurationTask>(TaskName.CREATE_DOKKA_MODULE_CONFIGURATION) {
            moduleName.set(project.name.map { it.takeIf(Char::isLetterOrDigit) ?: "-" }.joinToString(""))
            dokkaModuleConfigurationJson.set(
                moduleName.flatMap { moduleName ->
                    layout.buildDirectory.file("dokka/$moduleName.json")
                }
            )

            dependsOn(dokkaGenerateTask)

            moduleOutputDirectoryPath(dokkaGenerateTask.map { it.outputDirectory })
            // TODO what should the source-output be?
            sourceOutputDirectory(dokkaGenerateTask.map { it.outputDirectory })
//            sourceOutputDirectory(layout.buildDirectory.dir("dokka/source-output"))
        }
    }


    companion object {

        const val EXTENSION_NAME = "dokka"

        /**
         * Names of the Gradle [Configuration]s used by the Dokka Plugin.
         *
         * Beware the confusing terminology:
         * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] are used to share files between subprojects. Each has a name.
         * - [Dokka Configurations][org.jetbrains.dokka.DokkaConfiguration] are used to create JSON settings for the Dokka Generator
         */
        object ConfigurationName {

            const val DOKKA = "dokka"

            /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration] from projects */
            const val DOKKA_CONFIGURATIONS = "dokkaConfigurations"

            /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
            const val DOKKA_CONFIGURATION_ELEMENTS = "dokkaConfigurationElements"

            /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] from projects */
            const val DOKKA_MODULE_DESCRIPTORS = "dokkaModuleDescriptor"

            /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] to other projects */
            const val DOKKA_MODULE_DESCRIPTOR_ELEMENTS = "dokkaModuleDescriptorElements"

            const val DOKKA_RUNTIME_CLASSPATH = "dokkaRuntimeClasspath"
            const val DOKKA_PLUGINS_CLASSPATH = "dokkaPluginsClasspath"
        }

        /**
         * The group of all Dokka Gradle tasks.
         *
         * @see org.gradle.api.Task.getGroup
         */
        const val TASK_GROUP = "dokka"

        object TaskName {
            const val DOKKA_GENERATE = "dokkaGenerate"
            const val CREATE_DOKKA_CONFIGURATION = "createDokkaConfiguration"
            const val CREATE_DOKKA_MODULE_CONFIGURATION = "createDokkaModuleConfiguration"
        }

        internal val jsonMapper = Json {
            prettyPrint = true
        }

    }
}
