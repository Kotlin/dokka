package org.jetbrains.dokka.gradle

import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_GENERATOR_CLASSPATH
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import org.jetbrains.dokka.gradle.adapters.DokkaKotlinAdapter
import org.jetbrains.dokka.gradle.distibutions.DokkaPluginConfigurations
import org.jetbrains.dokka.gradle.distibutions.setupDokkaConfigurations
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaPluginConfigurationGradleBuilder
import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.dokka.gradle.tasks.DokkaModuleConfigurationTask
import javax.inject.Inject


abstract class DokkaPlugin @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
    private val objects: ObjectFactory,
) : Plugin<Project> {

    override fun apply(target: Project) {

        val dokkaSettings = target.extensions.create<DokkaPluginSettings>(EXTENSION_NAME).apply {
            dokkaVersion.convention("1.7.20")
            dokkaCacheDirectory.convention(target.rootProject.layout.buildDirectory.dir("dokka-cache"))
        }

        val dokkaConfigurations = target.setupDokkaConfigurations(dokkaSettings)

        target.addDokkaDependencies(dokkaSettings)

        val dokkaConfigurationTask = target.tasks.registerCreateDokkaConfigurationTask(
            dokkaSettings,
            dokkaConfigurations,
        )

        val dokkaGenerateTask = target.tasks.registerDokkaGenerateTask(
            dokkaSettings,
            dokkaConfigurationTask,
            dokkaConfigurations,
        )

        val dokkaModuleConfigurationTask = target.tasks.registerDokkaModuleConfigurationTask(dokkaGenerateTask)

        dokkaConfigurations.dokkaModuleDescriptorsElements.configure {
            outgoing {
                artifact(dokkaModuleConfigurationTask.map { it.dokkaModuleConfigurationJson })
            }
        }
        dokkaConfigurations.dokkaConfigurationsElements.configure {
            outgoing {
                artifact(dokkaConfigurationTask.flatMap { it.dokkaConfigurationJson })
            }
        }

        // apply the plugin that will autoconfigure Dokka to use the sources of a Kotlin project
        target.pluginManager.apply(DokkaKotlinAdapter::class)
    }

    private fun Project.addDokkaDependencies(dokkaSettings: DokkaPluginSettings) {

        //<editor-fold desc="DependencyHandler utils">
        fun DependencyHandlerScope.dokkaPlugin(dependency: Provider<Dependency>) =
            addProvider(DOKKA_PLUGINS_CLASSPATH, dependency)

        fun DependencyHandlerScope.dokkaPlugin(dependency: String) =
            add(DOKKA_PLUGINS_CLASSPATH, dependency)

        fun DependencyHandlerScope.dokkaGenerator(dependency: Provider<Dependency>) =
            addProvider(DOKKA_GENERATOR_CLASSPATH, dependency)

        fun DependencyHandlerScope.dokkaGenerator(dependency: String) =
            add(DOKKA_GENERATOR_CLASSPATH, dependency)
        //</editor-fold>

        dependencies {
            fun dokka(module: String) =
                dokkaSettings.dokkaVersion.map { version -> create("org.jetbrains.dokka:$module:$version") }

//            dokkaPluginsClasspath("org.jetbrains:markdown:0.3.1")
            dokkaPlugin("org.jetbrains:markdown-jvm:0.3.1")
            dokkaPlugin(dokka("kotlin-analysis-intellij"))
            dokkaPlugin(dokka("dokka-base"))
            dokkaPlugin(dokka("templating-plugin"))
            dokkaPlugin(dokka("dokka-analysis"))
            dokkaPlugin(dokka("kotlin-analysis-compiler"))

//            dokkaPluginsClasspath("org.jetbrains.kotlinx:kotlinx-html:0.8.0")
            dokkaPlugin("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
            dokkaPlugin("org.freemarker:freemarker:2.3.31")

            dokkaGenerator(dokka("dokka-core"))
        }
    }

    private fun TaskContainer.registerDokkaGenerateTask(
        dokkaSettings: DokkaPluginSettings,
        dokkaConfigurationTask: TaskProvider<DokkaConfigurationTask>,
        dokkaConfigurations: DokkaPluginConfigurations,
    ): TaskProvider<DokkaGenerateTask> {
        val generateTask = register<DokkaGenerateTask>(TaskName.DOKKA_GENERATE)

        withType<DokkaGenerateTask>().configureEach {
            dokkaConfigurationJson.set(dokkaConfigurationTask.flatMap { it.dokkaConfigurationJson })
            cacheDirectory.convention(dokkaSettings.dokkaCacheDirectory)
            outputDirectory.convention(dokkaConfigurationTask.flatMap { it.outputDir })

//            runtimeClasspath.from(dokkaConfigurations.dokkaPluginsClasspath.map(Configuration::resolve))
            runtimeClasspath.from(dokkaConfigurations.dokkaGeneratorClasspath.map(Configuration::resolve))
        }

        return generateTask
    }


    private fun TaskContainer.registerCreateDokkaConfigurationTask(
        dokkaSettings: DokkaPluginSettings,
        dokkaConfigurations: DokkaPluginConfigurations,
    ): TaskProvider<DokkaConfigurationTask> {
        val dokkaConfigurationTask = register<DokkaConfigurationTask>(TaskName.CREATE_DOKKA_CONFIGURATION)

        withType<DokkaConfigurationTask>().configureEach configTask@{

            cacheRoot.convention(dokkaSettings.dokkaCacheDirectory)
            delayTemplateSubstitution.convention(false)
            dokkaConfigurationJson.convention(layout.buildDirectory.file("dokka-config/dokka_configuration.json"))
            outputDir.convention(layout.buildDirectory.dir("dokka-output"))
            failOnWarning.convention(false)
            finalizeCoroutines.convention(false)
            suppressInheritedMembers.convention(false)
            suppressObviousFunctions.convention(false)
            offlineMode.convention(false)
            moduleName.convention(providers.provider { project.name })
            moduleVersion.convention(providers.provider { project.version.toString() })
            dokkaSourceSets.addAllLater(providers.provider { dokkaSettings.dokkaSourceSets })

            // helper for the old DSL,
            // todo I want to move dokkaSourceSets extension to the DokkaPluginSettings, out of tasks
            extensions.add("dokkaSourceSets", dokkaSourceSets)

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

            pluginsClasspath.from(dokkaConfigurations.dokkaPluginsIntransitiveClasspath.map {
                it.incoming.artifactView { }.files
            })

            dokkaSourceSets.configureEach {
                // TODO copy default values from old plugin
                jdkVersion.convention(11)
                noJdkLink.convention(true)
                noAndroidSdkLink.convention(true)
                noStdlibLink.convention(true)
                suppress.convention(true)
                reportUndocumented.convention(false)
                skipDeprecated.convention(false)
                skipEmptyPackages.convention(false)
                sourceSetScope.convention(this@configTask.path)
                suppressGeneratedFiles.convention(true)
                displayName.convention(this@configTask.path)
                analysisPlatform.convention(Platform.DEFAULT)

//                languageVersion.convention("1.7")
//                apiVersion.convention("1.7")

                sourceLinks.configureEach {
                    localDirectory.convention(layout.projectDirectory.asFile)
                    remoteLineSuffix.convention("#L")
                }

                perPackageOptions.configureEach {
                    matchingRegex.convention(".*")
                    suppress.convention(false)
                    skipDeprecated.convention(false)
                    reportUndocumented.convention(false)
                    @Suppress("DEPRECATION")
                    includeNonPublic.convention(false)
                }
            }

            pluginsConfiguration.configureEach {
//                serializationFormat.convention("JSON")
                serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
            }

            pluginsConfiguration.addAllLater(
                @Suppress("DEPRECATION")
                pluginsMapConfiguration.map { pluginConfig ->
                    pluginConfig.map { (pluginId, pluginConfiguration) ->
                        objects.newInstance<DokkaPluginConfigurationGradleBuilder>().apply {
                            fqPluginName.set(pluginId)
                            values.set(pluginConfiguration)
                        }
                    }
                }
            )
        }

        return dokkaConfigurationTask
    }


    private fun TaskContainer.registerDokkaModuleConfigurationTask(
        dokkaGenerateTask: TaskProvider<DokkaGenerateTask>,
    ): TaskProvider<DokkaModuleConfigurationTask> {
        val dokkaModuleConfigurationTask =
            register<DokkaModuleConfigurationTask>(TaskName.CREATE_DOKKA_MODULE_CONFIGURATION)

        withType<DokkaModuleConfigurationTask>().configureEach {
            moduleName.set(project.name.map { it.takeIf(Char::isLetterOrDigit) ?: "-" }.joinToString(""))
            dokkaModuleConfigurationJson.set(
                moduleName.flatMap { moduleName ->
                    layout.buildDirectory.file("dokka/$moduleName.json")
                }
            )

            dependsOn(dokkaGenerateTask)

//            moduleOutputDirectoryPath(dokkaGenerateTask.map { it.outputDirectory })
            sourceOutputDirectory(dokkaGenerateTask.map { it.outputDirectory })
//            sourceOutputDirectory(layout.buildDirectory.dir("dokka/source-output"))
        }

        return dokkaModuleConfigurationTask
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
            const val DOKKA_CONFIGURATIONS = "dokkaConfiguration"

            /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
            const val DOKKA_CONFIGURATION_ELEMENTS = "dokkaConfigurationElements"

            /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] from projects */
            const val DOKKA_MODULE_DESCRIPTORS = "dokkaModule"

            /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] to other projects */
            const val DOKKA_MODULE_DESCRIPTOR_ELEMENTS = "dokkaModuleDescriptors"

            /**
             * Classpath used to execute the Dokka Generator.
             *
             * Extends [DOKKA_PLUGINS_CLASSPATH], so Dokka plugins and their dependencies are included.
             */
            const val DOKKA_GENERATOR_CLASSPATH = "dokkaGeneratorClasspath"

            /** Dokka Plugins (transitive, so this can be passed to the Dokka Generator Worker classpath) */
            const val DOKKA_PLUGINS_CLASSPATH = "dokkaPlugin"

            /**
             * Dokka Plugins (excluding transitive dependencies, so this be used to create Dokka Generator Configuration
             *
             * Generally, this configuration should not be invoked manually. Instead, use [DOKKA_PLUGINS_CLASSPATH].
             */
            internal const val DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH = "${DOKKA_PLUGINS_CLASSPATH}Intransitive"
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
