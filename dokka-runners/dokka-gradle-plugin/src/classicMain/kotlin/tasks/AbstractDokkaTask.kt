/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.*
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.dokka.*
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaPlugin
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiConsumer
import kotlin.reflect.full.createInstance

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
abstract class AbstractDokkaTask : DefaultTask() {

    /**
     * Display name used to refer to the module. Used for ToC, navigation, logging, etc.
     *
     * If set for a single-project build or a MultiModule task, will be used as project name.
     *
     * Default is Gradle project name.
     */
    @Input
    val moduleName: Property<String> = project.objects.property<String>()
        .convention(project.name)

    /**
     * Module version.
     *
     * If set for a single-project build or a MultiModule task, will be used
     * as project version by the versioning plugin.
     *
     * Default is Gradle project version.
     */
    @Input
    val moduleVersion: Property<String> = project.objects.property<String>()
        .convention(project.provider { project.version.toString() })

    /**
     * Directory to which documentation will be generated, regardless of format.
     * Can be set on per-task basis.
     *
     * Default is `project/buildDir/taskName.removePrefix("dokka").decapitalize()`, so
     * for `dokkaHtmlMultiModule` task it will be `project/buildDir/htmlMultiModule`
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Configuration for Dokka plugins. This property is not expected to be used directly - if possible, use
     * [pluginConfiguration] blocks (preferred) or [pluginsMapConfiguration] instead.
     */
    @Input
    val pluginsConfiguration: ListProperty<in DokkaConfiguration.PluginConfiguration> = project.objects.listProperty()

    /**
     * JSON configuration of Dokka plugins.
     *
     * Key is fully qualified Dokka plugin name, value is its configuration in JSON.
     *
     * Example:
     *
     * ```kotlin
     * tasks.dokkaHtml {
     *     val dokkaBaseConfiguration = """
     *     {
     *         "customAssets": ["${file("assets/my-image.png")}"],
     *         "customStyleSheets": ["${file("assets/my-styles.css")}"],
     *         "footerMessage": "(c) 2022 MyOrg"
     *     }
     *     """
     *     pluginsMapConfiguration.set(
     *         mapOf("org.jetbrains.dokka.base.DokkaBase" to dokkaBaseConfiguration)
     *     )
     * }
     * ```
     */
    @Input
    val pluginsMapConfiguration: MapProperty<String, String> = project.objects.mapProperty()

    /**
     * Whether to suppress obvious functions.
     *
     * A function is considered to be obvious if it is:
     * - Inherited from `kotlin.Any`, `Kotlin.Enum`, `java.lang.Object` or `java.lang.Enum`,
     *   such as `equals`, `hashCode`, `toString`.
     * - Synthetic (generated by the compiler) and does not have any documentation, such as
     *   `dataClass.componentN` or `dataClass.copy`.
     *
     * Default is `true`
     */
    @Input
    val suppressObviousFunctions: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.suppressObviousFunctions)

    /**
     * Whether to suppress inherited members that aren't explicitly overridden in a given class.
     *
     * Note: this can suppress functions such as `equals`/`hashCode`/`toString`, but cannot suppress
     * synthetic functions such as `dataClass.componentN` and `dataClass.copy`. Use [suppressObviousFunctions]
     * for that.
     *
     * Default is `false`.
     */
    @Input
    val suppressInheritedMembers: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.suppressInheritedMembers)

    /**
     * Whether to resolve remote files/links over network.
     *
     * This includes package-lists used for generating external documentation links:
     * for instance, to make classes from standard library clickable.
     *
     * Setting this to `true` can significantly speed up build times in certain cases,
     * but can also worsen documentation quality and user experience, for instance by
     * not resolving some dependency's class/member links.
     *
     * When using offline mode, you can cache fetched files locally and provide them to
     * Dokka as local paths. For instance, see [GradleExternalDocumentationLinkBuilder].
     *
     * Default is `false`.
     */
    @Input
    val offlineMode: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.offlineMode)

    /**
     * Whether to fail documentation generation if Dokka has emitted a warning or an error.
     * Will wait until all errors and warnings have been emitted first.
     *
     * This setting works well with [GradleDokkaSourceSetBuilder.reportUndocumented]
     *
     * Default is `false`.
     */
    @Input
    val failOnWarning: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.failOnWarning)

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cacheRoot: DirectoryProperty

    /**
     * Type-safe configuration for a Dokka plugin.
     *
     * Note: this is available in Kotlin DSL only, if Dokka Gradle plugin was applied through `plugins` block
     * and the configured plugin can be found on classpath, which may require adding a classpath dependency
     * to `buildscript` block in case of external plugins. Some Dokka plugins, such as
     * [org.jetbrains.dokka.base.DokkaBase], are on classpath by default.
     *
     * Example:
     *
     * ```kotlin
     * import org.jetbrains.dokka.base.DokkaBase
     * import org.jetbrains.dokka.base.DokkaBaseConfiguration
     *
     * tasks.dokkaHtml {
     *     pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
     *         footerMessage = "Test"
     *     }
     * }
     * ```
     *
     * @param P Plugin class that extends [DokkaPlugin]
     * @param T Plugin configuration class that extends [ConfigurableBlock]
     */
    inline fun <reified P : DokkaPlugin, reified T : ConfigurableBlock> pluginConfiguration(block: T.() -> Unit) {
        val instance = T::class.createInstance().apply(block)
        val pluginConfiguration = PluginConfigurationImpl(
            fqPluginName = P::class.qualifiedName!!,
            serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
            values = instance.toCompactJsonString()
        )
        pluginsConfiguration.add(pluginConfiguration)
    }

    @Internal
    val plugins: Configuration = project.maybeCreateDokkaPluginConfiguration(name)

    /** Resolve the dependencies from [plugins]. */
    @get:Classpath
    @InternalDokkaGradlePluginApi
    abstract val pluginsClasspath: ConfigurableFileCollection

    @Internal
    val runtime: Configuration = project.maybeCreateDokkaRuntimeConfiguration(name)

    /** Resolve the dependencies from [runtime]. */
    @get:Classpath
    @InternalDokkaGradlePluginApi
    abstract val runtimeClasspath: ConfigurableFileCollection

    private val providers: ProviderFactory = project.providers

    /**
     * Internal Dokka Gradle Plugin only.
     *
     * Override the log level that [DokkaBootstrap] produces output logs. Intended for use in tests.
     */
    private val dokkaGeneratorLogLevel: Provider<LogLevel>
        get() = providers.gradleProperty("org.jetbrains.dokka.internal.gradleLogLevel")
            .flatMap {
                providers.provider {
                    LogLevel.values().firstOrNull { level -> level.name == it }
                }
            }

    final override fun doFirst(action: Action<in Task>): Task = super.doFirst(action)

    final override fun doFirst(action: Closure<*>): Task = super.doFirst(action)

    @TaskAction
    internal open fun generateDocumentation() {
        DokkaBootstrap(runtimeClasspath.files, DokkaBootstrapImpl::class).apply {
            configure(buildDokkaConfiguration().toCompactJsonString(), createProxyLogger())
            val uncaughtExceptionHolder = AtomicReference<Throwable?>()
            /**
             * Run in a new thread to avoid memory leaks that are related to ThreadLocal (that keeps `URLCLassLoader`)
             * Currently, all `ThreadLocal`s leaking are in the compiler/IDE codebase.
             */
            Thread { generate() }.apply {
                setUncaughtExceptionHandler { _, throwable -> uncaughtExceptionHolder.set(throwable) }
                start()
                join()
            }
            uncaughtExceptionHolder.get()?.let { throw it }
        }
    }

    internal abstract fun buildDokkaConfiguration(): DokkaConfigurationImpl

    private fun createProxyLogger(): BiConsumer<String, String> =
        object : BiConsumer<String, String> {
            private val overrideLogger: ((message: String) -> Unit)? =
                when (dokkaGeneratorLogLevel.orNull) {
                    DEBUG -> logger::debug
                    INFO -> logger::info
                    LIFECYCLE -> logger::lifecycle
                    WARN -> logger::warn
                    QUIET -> logger::quiet
                    ERROR -> logger::error
                    null -> null
                }

            override fun accept(level: String, message: String) {
                if (overrideLogger != null) {
                    // when an override level is set, re-route all the messages
                    overrideLogger.invoke(message)
                } else {
                    // otherwise, map the Dokka log level (org.jetbrains.dokka.utilities.LoggingLevel)
                    // to an equivalent Gradle log level
                    when (level) {
                        "debug" -> logger.debug(message)
                        "info" -> logger.info(message)
                        "progress" -> logger.lifecycle(message)
                        "warn" -> logger.warn(message)
                        "error" -> logger.error(message)
                    }
                }
            }
        }

    init {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        // notCompatibleWithConfigurationCache was introduced in Gradle 7.4
        if (GradleVersion.current() >= GradleVersion.version("7.4")) {
            super.notCompatibleWithConfigurationCache("Dokka tasks are not yet compatible with the Gradle configuration cache. See https://github.com/Kotlin/dokka/issues/1217")
        }
    }

    internal fun buildPluginsConfiguration(): List<PluginConfigurationImpl> {
        val manuallyConfigured = pluginsMapConfiguration.get().entries.map { entry ->
            PluginConfigurationImpl(
                entry.key,
                DokkaConfiguration.SerializationFormat.JSON,
                entry.value
            )
        }
        return pluginsConfiguration.get().mapNotNull { it as? PluginConfigurationImpl } + manuallyConfigured
    }
}
