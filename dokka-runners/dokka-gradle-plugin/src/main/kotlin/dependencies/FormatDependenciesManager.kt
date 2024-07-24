/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaClasspathAttribute
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaFormatAttribute
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.consumable
import org.jetbrains.dokka.gradle.internal.declarable
import org.jetbrains.dokka.gradle.internal.resolvable

/**
 * Dependencies for a specific Dokka Format - for example, HTML or Markdown.
 *
 * The [Configuration] here are used to declare, resolve, and share dependencies
 * from external sources (example: Maven Central), or between subprojects.
 *
 * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
 * [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration]
 * is used to configure Dokka behaviour.)
 */
@DokkaInternalApi
class FormatDependenciesManager(
    private val formatName: String,
    private val baseDependencyManager: BaseDependencyManager,
    private val project: Project,
    private val objects: ObjectFactory,
) {

    private val configurationNames = DependencyContainerNames(formatName)

    internal val baseAttributes: BaseAttributes = baseDependencyManager.baseAttributes

    internal val formatAttributes: FormatAttributes =
        FormatAttributes(
            formatName = formatName,
        )

    init {
        project.dependencies {
            applyAttributeHacks()
        }
    }

    private fun AttributeContainer.jvmJar() {
        attribute(USAGE_ATTRIBUTE, objects.named(AttributeHackPrefix + JAVA_RUNTIME))
        attribute(CATEGORY_ATTRIBUTE, objects.named(AttributeHackPrefix + LIBRARY))
        attribute(BUNDLING_ATTRIBUTE, objects.named(AttributeHackPrefix + EXTERNAL))
        attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(AttributeHackPrefix + STANDARD_JVM))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(AttributeHackPrefix + JAR))
    }

    //region Dokka Generator Plugins
    /**
     * Dokka plugins.
     *
     * Users can add plugins to this dependency.
     *
     * Should not contain runtime dependencies - use [dokkaGeneratorClasspath].
     */
    private val dokkaPluginsClasspath: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.pluginsClasspath) {
            description = "Dokka Plugins classpath for $formatName."
            declarable()
            extendsFrom(baseDependencyManager.dokkaGeneratorPlugins)
        }

    /**
     * Resolves Dokka Plugins, without transitive dependencies.
     *
     * It extends [dokkaPluginsClasspath].
     */
    val dokkaPluginsIntransitiveClasspathResolver: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.pluginsClasspathIntransitiveResolver) {
            description =
                "Resolves Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
            resolvable()
            extendsFrom(dokkaPluginsClasspath.get())
            isTransitive = false
            attributes {
                jvmJar()
                attribute(DokkaFormatAttribute, formatAttributes.format.name)
                attribute(DokkaClasspathAttribute, baseAttributes.dokkaPlugins.name)
            }
        }
    //endregion

    //region Dokka Plugins for Publication Generation
    private val dokkaPublicationPluginClasspath: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.publicationPluginClasspath) {
            description =
                "Dokka Plugins classpath for a $formatName Publication (consisting of 1+ Dokka Module)."
            declarable()
            extendsFrom(baseDependencyManager.declaredDependencies)
        }

    val dokkaPublicationPluginClasspathResolver: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.publicationPluginClasspathResolver) {
            description =
                "Resolves Dokka Plugins classpath for a $formatName Publication (consisting of 1+ Dokka Module)."
            resolvable()
            extendsFrom(dokkaPublicationPluginClasspath.get())
            attributes {
                jvmJar()
                attribute(DokkaFormatAttribute, formatAttributes.format.name)
                attribute(DokkaClasspathAttribute, baseAttributes.dokkaPublicationPlugins.name)
            }
        }

    val dokkaPublicationPluginClasspathApiOnly: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.publicationPluginClasspathApiOnly) {
            description =
                "Dokka Plugins for consumers that will assemble a $formatName Publication using the Dokka Module that this project produces"
            declarable()
        }

    init {
        project.configurations.register(configurationNames.publicationPluginClasspathApiOnlyConsumable) {
            description =
                "Shared Dokka Plugins for consumers that will assemble a $formatName Publication using the Dokka Module that this project produces"
            consumable()
            extendsFrom(dokkaPublicationPluginClasspathApiOnly.get())
            attributes {
                jvmJar()
                attribute(DokkaFormatAttribute, formatAttributes.format.name)
                attribute(DokkaClasspathAttribute, baseAttributes.dokkaPublicationPlugins.name)
            }
        }
    }
    //endregion

    //region Dokka Generator Classpath
    /**
     * Runtime classpath used to execute Dokka Worker.
     *
     * This configuration is not exposed to other subprojects.
     *
     * Extends [dokkaPluginsClasspath].
     *
     * @see org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker
     * @see org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
     */
    private val dokkaGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.generatorClasspath) {
            description =
                "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
            declarable()

            // extend from plugins classpath, so Dokka Worker can run the plugins
            extendsFrom(dokkaPluginsClasspath.get())
        }

    /**
     * Runtime classpath used to execute Dokka Worker.
     *
     * This configuration is not exposed to other subprojects.
     *
     * Extends [dokkaPluginsClasspath].
     *
     * @see org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker
     * @see org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
     */
    val dokkaGeneratorClasspathResolver: NamedDomainObjectProvider<Configuration> =
        project.configurations.register(configurationNames.generatorClasspathResolver) {
            description =
                "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
            resolvable()

            // extend from plugins classpath, so Dokka Worker can run the plugins
            extendsFrom(dokkaGeneratorClasspath.get())

            attributes {
                jvmJar()
                attribute(DokkaFormatAttribute, formatAttributes.format.name)
                attribute(DokkaClasspathAttribute, baseAttributes.dokkaGenerator.name)
            }
        }
    //endregion

    /**
     * Output directories of a Dokka Module.
     *
     * Contains
     *
     * - `module-descriptor.json`
     * - module output directory
     * - module includes directory
     *
     * @see org.jetbrains.dokka.gradle.dokka.parameters.DokkaModuleDescriptionKxs
     * @see org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
     */
    val moduleOutputDirectories: ModuleComponentDependencies =
        componentDependencies(formatAttributes.moduleOutputDirectories)

    private fun componentDependencies(
        component: DokkaAttribute.ModuleComponent,
    ): ModuleComponentDependencies =
        ModuleComponentDependencies(
            project = project,
            component = component,
            baseAttributes = baseAttributes,
            formatAttributes = formatAttributes,
            declaredDependencies = baseDependencyManager.declaredDependencies,
            baseConfigurationName = configurationNames.dokka,
        )
}
