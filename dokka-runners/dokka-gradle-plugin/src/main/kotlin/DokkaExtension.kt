/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.newInstance
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.gradle.dependencies.BaseDependencyManager
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.formats.DokkaPublication
import org.jetbrains.dokka.gradle.internal.*
import org.jetbrains.dokka.gradle.workers.ClassLoaderIsolation
import org.jetbrains.dokka.gradle.workers.ProcessIsolation
import org.jetbrains.dokka.gradle.workers.WorkerIsolation
import java.io.Serializable

/**
 * Configure the behaviour of the [DokkaBasePlugin].
 */
abstract class DokkaExtension
@DokkaInternalApi
constructor(
    private val objects: ObjectFactory,
    internal val baseDependencyManager: BaseDependencyManager,
) : ExtensionAware, Serializable {

    /** Directory into which [DokkaPublication]s will be produced */
    abstract val dokkaPublicationDirectory: DirectoryProperty

    /**
     * Directory into which Dokka Modules will be produced.
     *
     * Note that Dokka Modules are intermediate products and must be combined into a completed
     * Dokka Publication. They are not intended to be comprehensible in isolation.
     */
    abstract val dokkaModuleDirectory: DirectoryProperty

    /** Default Dokka Gradle Plugin cache directory */
    abstract val dokkaCacheDirectory: DirectoryProperty

    abstract val moduleName: Property<String>
    abstract val moduleVersion: Property<String>
    abstract val modulePath: Property<String>

    /**
     * An arbitrary string used to group source sets that originate from different Gradle subprojects.
     *
     * This is primarily used by Kotlin Multiplatform projects, which can have multiple source sets
     * per subproject.
     *
     * Defaults to [the Gradle path of the subproject][org.gradle.api.Project.getPath].
     */
    abstract val sourceSetScopeDefault: Property<String>

    /**
     * The Konan home directory, which contains libraries for Kotlin/Native development.
     *
     * This is only required as a workaround to fetch the compile-time dependencies in Kotlin/Native
     * projects with a version below 2.0.
     */
    // This property should be removed when Dokka only supports KGP 2 or higher.
    @DokkaInternalApi
    abstract val konanHome: RegularFileProperty

    /**
     * Configuration for creating Dokka Publications.
     *
     * Each publication will generate one Dokka site based on the included Dokka Source Sets.
     *
     * The type of site is determined by the Dokka plugins. By default, an HTML site will be generated.
     */
    val dokkaPublications: NamedDomainObjectContainer<DokkaPublication> =
        extensions.adding(
            "dokkaPublications",
            objects.domainObjectContainer { named -> objects.newInstance(named, pluginsConfiguration) }
        )

    /**
     * Dokka Source Sets describe the source code that should be included in a Dokka Publication.
     *
     * Dokka will not generate documentation unless there is at least there is at least one Dokka Source Set.
     *
     *  TODO make sure dokkaSourceSets doc is up to date...
     *
     * Only source sets that are contained within _this project_ should be included here.
     * To merge source sets from other projects, use the Gradle dependencies block.
     *
     * ```kotlin
     * dependencies {
     *   // merge :other-project into this project's Dokka Configuration
     *   dokka(project(":other-project"))
     * }
     * ```
     *
     * Or, to include other Dokka Publications as a Dokka Module use
     *
     * ```kotlin
     * dependencies {
     *   // include :other-project as a module in this project's Dokka Configuration
     *   dokkaModule(project(":other-project"))
     * }
     * ```
     *
     * Dokka will merge Dokka Source Sets from other subprojects if...
     */
    val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
        extensions.adding("dokkaSourceSets", objects.domainObjectContainer())

    /**
     * Dokka Plugin are used to configure the way Dokka generates a format.
     * Some plugins can be configured via parameters, and those parameters are stored in this
     * container.
     */
    val pluginsConfiguration: DokkaPluginParametersContainer =
        extensions.adding("pluginsConfiguration", objects.dokkaPluginParametersContainer())

    /**
     * The default version of Dokka dependencies that are used at runtime during generation.
     *
     * This value defaults to the current Dokka Gradle Plugin version, but can be overridden
     * if you want to use a newer or older version of Dokka at runtime.
     */
    abstract val dokkaEngineVersion: Property<String>

    /**
     * Dokka Gradle Plugin runs Dokka Generator in a separate
     * [Gradle Worker](https://docs.gradle.org/8.5/userguide/worker_api.html).
     *
     * You can control whether Dokka Gradle Plugin launches Dokka Generator in
     * * a new process, using [ProcessIsolation],
     * * or the current process with an isolated classpath, using [ClassLoaderIsolation].
     *
     * _Aside: Launching [without isolation][WorkerExecutor.noIsolation] is not an option, because
     * running Dokka Generator **requires** an isolated classpath._
     *
     * ```kotlin
     * dokka {
     *   // use the current Gradle process, but with an isolated classpath
     *   workerIsolation = ClassLoaderIsolation()
     *
     *   // launch a new process, optionally controlling the standard JVM options
     *   workerIsolation = ProcessIsolation {
     *     minHeapSize = "2g" // increase minimum heap size
     *     systemProperties.add("someCustomProperty", 123)
     *   }
     * }
     * ```
     *
     * @see WorkerIsolation
     * @see org.jetbrains.dokka.gradle.workers.ProcessIsolation
     * @see org.jetbrains.dokka.gradle.workers.ClassLoaderIsolation
     *
     */
    @get:Nested
    abstract val dokkaGeneratorIsolation: Property<WorkerIsolation>

    /**
     * Create a new [ClassLoaderIsolation] options instance.
     *
     * The resulting options must be set into [dokkaGeneratorIsolation].
     */
    fun ClassLoaderIsolation(configure: ClassLoaderIsolation.() -> Unit = {}): ClassLoaderIsolation =
        objects.newInstance<ClassLoaderIsolation>().apply(configure)

    /**
     * Create a new [ProcessIsolation] options.
     *
     * The resulting options instance must be set into [dokkaGeneratorIsolation].
     */
    fun ProcessIsolation(configure: ProcessIsolation.() -> Unit = {}): ProcessIsolation =
        objects.newInstance<ProcessIsolation>().apply(configure)


    //region deprecated properties
    /**
     * ```
     * dokka {
     *   // DEPRECATED
     *   suppressInheritedMembers.set(true)
     *
     *   // Replace with:
     *   dokkaPublications.configureEach {
     *     suppressInheritedMembers.set(true)
     *   }
     * }
     * ```
     * @see DokkaPublication.suppressInheritedMembers
     */
    @Deprecated("Moved to DokkaPublication#suppressInheritedMembers")
    abstract val suppressInheritedMembers: Property<Boolean>

    /**
     * ```
     * dokka {
     *   // DEPRECATED
     *   suppressObviousFunctions.set(true)
     *
     *   // Replace with:
     *   dokkaPublications.configureEach {
     *     suppressObviousFunctions.set(true)
     *   }
     * }
     * ```
     *
     * @see DokkaPublication.suppressObviousFunctions
     */
    @Deprecated("Moved to DokkaPublication#suppressObviousFunctions")
    abstract val suppressObviousFunctions: Property<Boolean>

    /**
     * JSON configuration of Dokka plugins is deprecated.
     * Typesafe configuration must be used instead - see [pluginsConfiguration].
     *
     * In DPGv1 the Dokka plugins could be configured by manually writing JSON.
     * This caused issues with registering task inputs for Gradle up-to-date checks.
     * (For more information on registering task inputs, see
     * [Gradle Docs: Incremental build](https://docs.gradle.org/current/userguide/incremental_build.html)).
     *
     * In DGPv2 Dokka plugins must be configured in a typesafe way, using [pluginsConfiguration].
     *
     * #### Configuration of built-in Dokka plugins
     *
     * The built-in Dokka plugins can be configured using a typesafe DSL.
     *
     * This example demonstrates how to convert JSON configuration of the
     * [Dokka Versioning plugin](https://kotl.in/dokka-versioning-plugin)
     * into the new, typesafe config.
     *
     * ```
     * // Deprecated configuration of the Dokka Versioning plugin:
     * tasks.dokkaHtmlMultiModule {
     *     pluginsMapConfiguration.set(
     *         mapOf(
     *             "org.jetbrains.dokka.versioning.VersioningPlugin" to """
     *                 { "version": "1.2", "olderVersionsDir": "$projectDir/dokka-docs" }
     *             """.trimIndent()
     *         )
     *     )
     * }
     *
     * // New configuration in DGPv2 is typesafe and compatible with incremental builds.
     * dokka {
     *     pluginsConfiguration {
     *         versioning {
     *             version.set("1.2")
     *             olderVersionsDir.set(projectDir.resolve("dokka-docs"))
     *         }
     *     }
     * }
     * ```
     *
     * #### External Dokka Plugin configuration
     *
     * To configure external Dokka plugins you must create a subclass of
     * [DokkaPluginParametersBaseSpec][org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec],
     * and register it as a configuration type using
     * [pluginsConfiguration.registerBinding][org.gradle.api.ExtensiblePolymorphicDomainObjectContainer.registerBinding].
     *
     * ```
     * import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
     *
     * @OptIn(DokkaInternalApi::class)
     * abstract class MyCustomDokkaPluginConfiguration @Inject constructor(
     *     name: String
     * ) : DokkaPluginParametersBaseSpec(name, "demo.MyCustomDokkaPlugin") {
     *
     *     @get:Input
     *     @get:Optional
     *     abstract val flags: ListProperty<String>
     *
     *     override fun jsonEncode(): String {
     *         // convert the 'flags' to JSON, to be decoded by MyCustomDokkaPlugin.
     *     }
     * }
     *
     * dokka {
     *     pluginsConfiguration {
     *         registerBinding(MyCustomDokkaPluginConfiguration::class, MyCustomDokkaPluginConfiguration::class)
     *         register<MyCustomDokkaPluginConfiguration>("MyCustomDokkaPlugin") {
     *             flags.add("someFlag...")
     *         }
     *     }
     * }
     * ```
     *
     * @see pluginsConfiguration
     */
    @Deprecated(
        message = "JSON configuration of Dokka plugins is deprecated. Typesafe configuration must be used instead.",
        level = DeprecationLevel.ERROR,
    )
    @Suppress("unused")
    abstract val pluginsMapConfiguration: MapProperty<String, String>
    //endregion
}
