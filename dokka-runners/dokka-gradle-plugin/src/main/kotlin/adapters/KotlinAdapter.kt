/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.adapters

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.adapters.KotlinAdapter.Companion.currentKotlinToolingVersion
import org.jetbrains.dokka.gradle.adapters.KotlinAdapter.Companion.logKgpClassNotFoundWarning
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import org.jetbrains.dokka.gradle.internal.*
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.pluginFeaturesService
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import javax.inject.Inject

/**
 * The [KotlinAdapter] plugin will automatically register Kotlin source sets as Dokka source sets.
 *
 * This is an internal Dokka plugin and should not be used externally.
 * It is not a standalone plugin, it requires [DokkaBasePlugin] is also applied.
 */
@InternalDokkaGradlePluginApi
abstract class KotlinAdapter @Inject constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(project: Project) {
        logger.info("Applying $dkaName to ${project.path}")

        val kotlinExtension = project.findKotlinExtension()
        if (kotlinExtension == null) {
            logger.info("Skipping applying $dkaName in ${project.path} - could not find KotlinProjectExtension")
            return
        }
        logger.info("Configuring $dkaName in Gradle Kotlin Project ${project.path}")

        val dokkaExtension = project.extensions.getByType<DokkaExtension>()

        // first fetch the relevant properties of all KotlinCompilations
        val compilationDetailsBuilder = KotlinCompilationDetailsBuilder(
            providers = providers,
            objects = objects,
            konanHome = dokkaExtension.konanHome.asFile,
            project = project,
        )
        val allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails> =
            compilationDetailsBuilder.createCompilationDetails(
                kotlinProjectExtension = kotlinExtension,
            )

        // second, fetch the relevant properties of the Kotlin source sets
        val sourceSetDetailsBuilder = KotlinSourceSetDetailsBuilder(
            providers = providers,
            objects = objects,
            sourceSetScopeDefault = dokkaExtension.sourceSetScopeDefault,
            projectPath = project.path,
            project = project,
        )
        val sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails> =
            sourceSetDetailsBuilder.createSourceSetDetails(
                kotlinSourceSets = kotlinExtension.sourceSets,
                allKotlinCompilationDetails = allKotlinCompilationDetails,
            )

        // for each Kotlin source set, register a Dokka source set
        registerDokkaSourceSets(
            projectPath = project.path,
            dokkaExtension = dokkaExtension,
            sourceSetDetails = sourceSetDetails,
        )
    }

    /** Register a [DokkaSourceSetSpec] for each element in [sourceSetDetails]. */
    private fun registerDokkaSourceSets(
        projectPath: String,
        dokkaExtension: DokkaExtension,
        sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails>,
    ) {
        // proactively use 'all' so source sets will be available in users' build files if they use `named("...")`
        sourceSetDetails.all details@{
            dokkaExtension.dokkaSourceSets.register(
                projectPath = projectPath,
                details = this@details,
            )
        }
    }

    /** Register a single [DokkaSourceSetSpec] for [details]. */
    private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.register(
        projectPath: String,
        details: KotlinSourceSetDetails,
    ) {
        val kssPlatform = determineKotlinPlatform(projectPath, details)
        val kssClasspath = determineClasspath(details = details)

        register(details.name) dss@{
            suppress.convention(!details.isPublishedSourceSet())
            sourceRoots.from(details.sourceDirectories)
            classpath.from(kssClasspath)
            analysisPlatform.convention(kssPlatform)
            dependentSourceSets.addAllLater(details.dependentSourceSetIds)
        }
    }

    private fun determineKotlinPlatform(
        projectPath: String,
        details: KotlinSourceSetDetails,
    ): Provider<KotlinPlatform> {
        return details.allAssociatedCompilations.map { compilations: List<KotlinCompilationDetails> ->
            val allPlatforms = compilations
                // Exclude metadata compilations: they are always KotlinPlatform.Common, which isn't relevant here.
                // Dokka only cares about the compilable KMP targets of a KotlinSourceSet.
                .filter { !it.isMetadata }
                .map { it.kotlinPlatform }
                .distinct()

            val singlePlatform = allPlatforms.singleOrNull()

            if (singlePlatform == null) {
                val defaultPlatform =
                    if (allPlatforms.all { it == KotlinPlatform.JVM || it == KotlinPlatform.AndroidJVM }) {
                        KotlinPlatform.JVM
                    } else {
                        KotlinPlatform.Common
                    }
                logger.info(
                    "[$projectPath] Dokka could not determine KotlinPlatform for ${details.name} from targets ${compilations.map { it.target }}. " +
                            "Dokka will assume this is a ${defaultPlatform} source set. " +
                            "(All platforms: $allPlatforms)"
                )
                defaultPlatform
            } else {
                singlePlatform
            }
        }
    }

    private fun determineClasspath(
        details: KotlinSourceSetDetails,
    ): FileCollection {

        val primaryClasspath: Provider<FileCollection> =
            details.allAssociatedCompilations.zip(details.primaryCompilations) { allAssociatedCompilations, primaryCompilations ->
                val classpath = objects.fileCollection()

                if (primaryCompilations.isNotEmpty()) {
                    primaryCompilations.forEach { compilation ->
                        classpath.from(compilation.compilationClasspath)
                    }
                } else {
                    // handle a 'bamboo' source sets
                    // (intermediate source set with only one actual target,
                    // e.g. windowsMain and mingwMain only have one target: mingwX64Main)
                    allAssociatedCompilations.singleOrNull()?.let { compilation ->
                        classpath.from(compilation.compilationClasspath)
                    }
                }

                classpath
            }

        return objects.fileCollection()
            .from(primaryClasspath)
            .from(details.transformedMetadataDependencies)
    }

    @InternalDokkaGradlePluginApi
    companion object {
        private val dkaName: String = KotlinAdapter::class.simpleName!!

        private val logger = Logging.getLogger(KotlinAdapter::class.java)

        /** Get the version of the Kotlin Gradle Plugin currently used to compile the project. */
        // Must be lazy, else tests fail (because the KGP plugin isn't accessible)
        internal val currentKotlinToolingVersion: KotlinToolingVersion by lazy {
            val kgpVersion = getKotlinPluginVersion(logger)
            KotlinToolingVersion(kgpVersion)
        }

        /**
         * Applies [KotlinAdapter] to the current project when any plugin of type [KotlinBasePlugin]
         * is applied.
         *
         * [KotlinBasePlugin] is the parent type for the Kotlin/JVM, Kotlin/Multiplatform, Kotlin/JS plugins,
         * as well as AGP's kotlin-built-in plugin.
         */
        internal fun applyTo(project: Project) {
            findKotlinBasePlugins(project)?.all {
                project.pluginManager.apply(KotlinAdapter::class)
            }
        }

        /**
         * Tries fetching all plugins with type [KotlinBasePlugin],
         * returning `null` if the class is not available in the current classloader.
         *
         * (The class might not be available if the current project is a Java or Android project,
         * or the buildscripts have an inconsistent classpath https://github.com/gradle/gradle/issues/27218)
         */
        private fun findKotlinBasePlugins(project: Project): DomainObjectCollection<KotlinBasePlugin>? {
            return try {
                project.plugins.withType<KotlinBasePlugin>()
            } catch (ex: Throwable) {
                when (ex) {
                    is ClassNotFoundException,
                    is NoClassDefFoundError -> {
                        logKgpClassNotFoundWarning(
                            project,
                            kotlinBasePluginNotFoundException = ex,
                        )
                        null
                    }

                    else -> throw ex
                }
            }
        }

        /**
         * Check all plugins to see if they are a subtype of [KotlinBasePlugin].
         * If any are, log a warning.
         *
         * Also, log an info message with the stacktrace of [kotlinBasePluginNotFoundException].
         *
         * ##### Motivation
         *
         * If the buildscript classpath is inconsistent, it might not be possible for DGP
         * to react to KGP because the [KotlinBasePlugin] class can't be loaded.
         * If so, DGP will be lenient and not cause errors,
         * but it must display a prominent warning to help users find the problem.
         *
         * @param[kotlinBasePluginNotFoundException] The exception thrown when [KotlinBasePlugin] is not available.
         */
        private fun logKgpClassNotFoundWarning(
            project: Project,
            kotlinBasePluginNotFoundException: Throwable,
        ) {
            // hide the stacktrace at `--info` log level, to avoid flooding the log
            logger.info(
                "Dokka Gradle Plugin could not load KotlinBasePlugin in ${project.displayName}",
                kotlinBasePluginNotFoundException,
            )

            /**
             * Keep track of which projects have been warned by [logKgpClassNotFoundWarning],
             * otherwise it'll log the same warning multiple times for the same project, which is annoying.
             *
             * The warning can be logged multiple times if a project has both
             * `org.jetbrains.dokka` and `org.jetbrains.dokka-javadoc` applied.
             */
            fun checkIfAlreadyWarned(): Boolean {
                val key = "DOKKA INTERNAL - projectsWithKgpClassNotFoundWarningApplied"
                if (project.extra.has(key)) {
                    return true
                } else {
                    project.extra.set(key, true)
                    return false
                }
            }

            PluginIds.kotlin.forEach { pluginId ->
                project.pluginManager.withPlugin(pluginId) {
                    if (checkIfAlreadyWarned()) return@withPlugin
                    logger.warn(
                        """
                        |warning: Dokka could not load KotlinBasePlugin in ${project.displayName}, even though plugin $pluginId is applied.
                        |The most common cause is a Gradle limitation: the plugins applied to subprojects should be consistent.
                        |Please try the following:
                        |1. Apply the Dokka and Kotlin plugins to the root project using the `plugins {}` DSL.
                        |   (If the root project does not need the plugins, use 'apply false')
                        |2. Remove the Dokka and Kotlin plugins versions in the subprojects.
                        |For more information see:
                        | - https://docs.gradle.org/current/userguide/plugins_intermediate.html#sec:plugins_apply
                        | - https://github.com/gradle/gradle/issues/25616
                        | - https://github.com/gradle/gradle/issues/35117
                        |Please report any feedback or problems https://kotl.in/dokka-issues
                        |""".trimMargin()
                    )
                }
            }
        }
    }
}


/**
 * Store the details of all [KotlinCompilation]s in a configuration cache compatible way.
 *
 * The compilation details may come from a multiplatform project ([KotlinMultiplatformExtension])
 * or a single-platform project ([KotlinSingleTargetExtension]).
 */
@InternalDokkaGradlePluginApi
private data class KotlinCompilationDetails(
    /** [KotlinCompilation.target] name. */
    val target: String,

    /** `true` if the compilation is 'metadata'. See [KotlinMetadataTarget]. */
    val isMetadata: Boolean,

    /** [KotlinCompilation.platformType] name. */
    val kotlinPlatform: KotlinPlatform,

    /** The names of [KotlinCompilation.kotlinSourceSets]. */
    val primarySourceSetNames: Set<String>,

    /** The names of [KotlinCompilation.allKotlinSourceSets]. */
    val allSourceSetNames: Set<String>,

    /**
     * Whether the compilation is published or not.
     *
     * By default, only published compilations should be documented.
     *
     * (E.g. 'main' compilations are published, 'test' compilations are not.)
     */
    val publishedCompilation: Provider<Boolean>,

    /** [KotlinCompilation.kotlinSourceSets] → [KotlinSourceSet.dependsOn] names. */
    val dependentSourceSetNames: Set<String>,

    val compilationClasspath: FileCollection,

    /** [KotlinCompilation.defaultSourceSet] name. */
    val defaultSourceSetName: String,
)


/** Utility class, encapsulating logic for building [KotlinCompilationDetails]. */
private class KotlinCompilationDetailsBuilder(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val konanHome: Provider<File>,
    private val project: Project,
) {
    private val androidComponentsInfo: Provider<Set<AndroidVariantInfo>> = getAgpVariantInfo(project)

    fun createCompilationDetails(
        kotlinProjectExtension: KotlinProjectExtension,
    ): ListProperty<KotlinCompilationDetails> {

        val details = objects.listProperty<KotlinCompilationDetails>()

        details.addAll(
            providers.provider {
                kotlinProjectExtension
                    .allKotlinCompilations()
                    .map { compilation ->
                        createCompilationDetails(compilation = compilation)
                    }
            })

        return details
    }

    /**
     * Collect information about Android variants.
     * Used to determine whether a source set is published or not.
     * See [KotlinSourceSetDetails.isPublishedSourceSet].
     *
     * Android variant info must be fetched eagerly,
     * since AGP doesn't provide a lazy way of accessing component information.
     *
     * @see collectAndroidVariants
     */
    private fun getAgpVariantInfo(
        project: Project,
    ): Provider<Set<AndroidVariantInfo>> {
        val androidVariants = objects.setProperty(AndroidVariantInfo::class)

        PluginIds.android.forEach { pluginId ->
            project.pluginManager.withPlugin(pluginId) {
                collectAndroidVariants(project, androidVariants)
            }
        }

        return androidVariants
    }

    /** Create a single [KotlinCompilationDetails] for [compilation]. */
    private fun createCompilationDetails(
        compilation: KotlinCompilation<*>,
    ): KotlinCompilationDetails {

        val primarySourceSetNames = compilation.kotlinSourceSets.map { it.name }
        val allSourceSetNames = compilation.allKotlinSourceSets.map { it.name }
        val dependentSourceSetNames = compilation.kotlinSourceSets.flatMap { it.dependsOn }.map { it.name }

        val compilationClasspath: FileCollection =
            collectKotlinCompilationClasspath(compilation = compilation)

        return KotlinCompilationDetails(
            target = compilation.target.name,
            kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
            primarySourceSetNames = primarySourceSetNames.toSet(),
            allSourceSetNames = allSourceSetNames.toSet(),
            publishedCompilation = compilation.isPublished(),
            dependentSourceSetNames = dependentSourceSetNames.toSet(),
            compilationClasspath = compilationClasspath,
            defaultSourceSetName = compilation.defaultSourceSet.name,
            isMetadata = compilation.target is KotlinMetadataTarget,
        )
    }

    private fun KotlinProjectExtension.allKotlinCompilations(): Collection<KotlinCompilation<*>> =
        when (this) {
            is KotlinMultiplatformExtension -> targets
                .flatMap { it.compilations }
                // Exclude legacy KMP metadata compilations, only present in KGP 1.8 (they were retained to support DGPv1)
                .filterNot { it.platformType == KotlinPlatformType.common && it.name == MAIN_COMPILATION_NAME }

            is KotlinSingleTargetExtension<*> -> target.compilations

            else -> emptyList() // shouldn't happen?
        }

    /**
     * Get the [Configuration][org.gradle.api.artifacts.Configuration] names of all configurations
     * used to build this [KotlinCompilation] and
     * [its source sets][KotlinCompilation.kotlinSourceSets].
     */
    private fun collectKotlinCompilationClasspath(
        compilation: KotlinCompilation<*>,
    ): FileCollection {
        val compilationClasspath = objects.fileCollection()

        compilationClasspath.from(
            kotlinNativeDependencies(compilation)
        )

        if (compilation.target.platformType == androidJvm) {
            compilationClasspath.from(kotlinCompileDependencyFiles(compilation, "jar"))
            compilationClasspath.from(kotlinCompileDependencyFiles(compilation, "android-classes-jar"))
        } else {
            // using compileDependencyFiles breaks Android projects because AGP
            // fills it with files from many Configurations, and Gradle encounters variant resolution errors.
            compilationClasspath.from({ compilation.compileDependencyFiles })
        }

        return compilationClasspath
    }

    private fun kotlinCompileDependencyFiles(
        compilation: KotlinCompilation<*>,
        /** `android-classes-jar` or `jar` */
        artifactType: String,
    ): Provider<FileCollection> {
        return project.configurations
            .named(compilation.compileDependencyConfigurationName)
            .map {
                it.incoming
                    .artifactView {
                        // Android publishes many variants, which can cause Gradle to get confused,
                        // so specify that we need a JAR and resolve leniently
                        if (compilation.target.platformType == androidJvm) {
                            attributes { artifactType(artifactType) }

                            // Setting lenient=true is not ideal, because it might hide problems.
                            // Unfortunately, Gradle has no chill and dependency resolution errors
                            // will cause Dokka tasks to completely fail, even if the dependencies aren't necessary.
                            // (There's a chance that the dependencies aren't even used in the project!)
                            // So, resolve leniently to at least permit generating _something_,
                            // even if the generated output might be incomplete and missing some classes.
                            lenient(true)
                        }
                        // 'Regular' Kotlin compilations have non-JAR files (e.g. Kotlin/Native klibs),
                        // so don't add attributes for non-Android projects.
                    }
                    .artifacts
                    .artifactFiles
            }
    }

    private fun kotlinNativeDependencies(
        compilation: KotlinCompilation<*>,
    ): Provider<FileCollection> {

        // apply workaround for Kotlin/Native, which will be fixed in Kotlin 2.0
        // (see KT-61559: K/N dependencies will be part of `compilation.compileDependencyFiles`)
        return if (
            currentKotlinToolingVersion < KotlinToolingVersion("2.0.0")
            &&
            compilation is AbstractKotlinNativeCompilation
        ) {
            konanHome.map { konanHome ->
                val konanDistribution = KonanDistribution(konanHome)

                val dependencies = objects.fileCollection()

                dependencies.from(konanDistribution.stdlib)

                // Konan library files for a specific target
                dependencies.from(
                    konanDistribution.platformLibsDir
                        .resolve(compilation.konanTarget.name)
                        .listFiles()
                        .orEmpty()
                        .filter { it.isDirectory || it.extension == "klib" }
                )
            }
        } else {
            return providers.provider { objects.fileCollection() }
        }
    }

    /**
     * Determine if a [KotlinCompilation] is 'publishable', and so should be enabled by default
     * when creating a Dokka publication.
     *
     * Typically, 'main' compilations are publishable and 'test' compilations should be suppressed.
     * This can be overridden manually, though.
     *
     * @see DokkaSourceSetSpec.suppress
     */
    private fun KotlinCompilation<*>.isPublished(): Provider<Boolean> {
        return when (this) {
            is KotlinMetadataCompilation<*> ->
                providers.provider { true }

            is KotlinJvmAndroidCompilation -> {
                isJvmAndroidPublished(this)
            }

            else ->
                providers.provider { name == MAIN_COMPILATION_NAME }
        }
    }

    private fun isJvmAndroidPublished(
        compilation: KotlinJvmAndroidCompilation,
    ): Provider<Boolean> {
        return androidComponentsInfo.map { components ->
            val compilationComponents = components.filter { it.name == compilation.name }
            val result = compilationComponents.any { component -> component.hasPublishedComponent }
            logger.info {
                "[KotlinAdapter isJvmAndroidPublished] ${compilation.name} publishable:$result, compilationComponents:$compilationComponents"
            }
            result
        }
    }

    companion object {
        private val logger: Logger = Logging.getLogger(KotlinAdapter::class.java)
    }
}


/**
 * Store the details of all [KotlinSourceSet]s in a configuration cache compatible way.
 *
 * @param[named] Should be [KotlinSourceSet.getName]
 */
@InternalDokkaGradlePluginApi
private abstract class KotlinSourceSetDetails @Inject constructor(
    private val named: String,
) : Named {

    /** Direct source sets that this source set depends on. */
    abstract val dependentSourceSetIds: SetProperty<SourceSetIdSpec>
    abstract val sourceDirectories: ConfigurableFileCollection

    /** _All_ source directories from any (recursively) dependant source set. */
    abstract val sourceDirectoriesOfDependents: ConfigurableFileCollection

    /**
     * The specific compilations used to build this source set.
     *
     * (Typically there will only be one, but KGP permits manually registering more.)
     */
    abstract val primaryCompilations: ListProperty<KotlinCompilationDetails>

    /**
     * Associated compilations that this [KotlinSourceSet] participates in.
     *
     * For example, the compilation for `commonMain` will also participate in compiling
     * the leaf `linuxX64`, as well as the intermediate compilations of `nativeMain`, `linuxMain`, etc.
     */
    abstract val allAssociatedCompilations: ListProperty<KotlinCompilationDetails>

    /**
     * Workaround for KT-80551.
     *
     * See [org.jetbrains.dokka.gradle.adapters.TransformedMetadataDependencyProvider].
     */
    abstract val transformedMetadataDependencies: ConfigurableFileCollection

    /**
     * Estimate if this Kotlin source set contains 'published' (non-test) sources.
     *
     * @see KotlinCompilationDetails.publishedCompilation
     */
    fun isPublishedSourceSet(): Provider<Boolean> =
        allAssociatedCompilations.map { values ->
            values.any { it.publishedCompilation.get() }
        }

    override fun getName(): String = named
}


/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinSourceSetDetailsBuilder(
    private val sourceSetScopeDefault: Provider<String>,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    /** [Project.getPath]. Used for configuration-cache safe logging. */
    private val projectPath: String,
    private val project: Project,
) {
    private val logger = Logging.getLogger(KotlinSourceSetDetails::class.java)

    private val transformedMetadataDependencyProvider: TransformedMetadataDependencyProvider? by lazy {
        if (project.pluginFeaturesService.enableWorkaroundKT80551.get()) {
            logger.info("$projectPath TransformedMetadataDependencyProvider is enabled")
            TransformedMetadataDependencyProvider(project)
        } else {
            logger.info("$projectPath TransformedMetadataDependencyProvider is disabled")
            null
        }
    }

    fun createSourceSetDetails(
        kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
        allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
    ): NamedDomainObjectContainer<KotlinSourceSetDetails> {

        val sourceSetDetails = objects.domainObjectContainer { name ->
            objects.newInstance<KotlinSourceSetDetails>(name)
        }

        kotlinSourceSets.configureEach kss@{
            sourceSetDetails.register(
                kotlinSourceSet = this,
                allKotlinCompilationDetails = allKotlinCompilationDetails,
            )
        }

        return sourceSetDetails
    }

    /** Register a [DokkaSourceSetSpec]. */
    private fun NamedDomainObjectContainer<KotlinSourceSetDetails>.register(
        kotlinSourceSet: KotlinSourceSet,
        allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
    ) {
        val extantSourceDirectories = providers.provider {
            kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }
        }

        val primaryCompilations = allKotlinCompilationDetails.map { allCompilations ->
            allCompilations.filter { compilation ->
                kotlinSourceSet.name in compilation.primarySourceSetNames
            }
        }

        val allAssociatedCompilations = allKotlinCompilationDetails.map { allCompilations ->
            allCompilations.filter { compilation ->
                kotlinSourceSet.name in compilation.allSourceSetNames
            }
        }

        // Determine the source sets IDs of _other_ source sets that _this_ source depends on.
        // Do not include transitive dependencies.
        // (For example, linuxX64 depends on linuxMain, nativeMain, and commonMain,
        // but only _directly_ depends on linuxMain, so dependentSourceSets should only contain linuxMain.)
        val dependentSourceSets = providers.provider { kotlinSourceSet.dependsOn }

        val dependentSourceSetIds =
            providers.zip(
                dependentSourceSets,
                sourceSetScopeDefault,
            ) { sourceSets, sourceSetScope ->
                logger.info("[$projectPath] source set ${kotlinSourceSet.name} has ${sourceSets.size} dependents ${sourceSets.joinToString { it.name }}")
                sourceSets.map { dependedKss ->
                    objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
                }
            }

        val sourceDirectoriesOfDependents = kotlinSourceSet
            .allDependsOnSourceSets()
            .map { allDependsOns ->
                allDependsOns.fold(objects.fileCollection()) { acc, sourceSet ->
                    acc.from(sourceSet.kotlin.sourceDirectories)
                }
            }

        val transformedMetadataDependencies =
            allAssociatedCompilations.map { associated ->
                if (associated.all { it.kotlinPlatform == KotlinPlatform.Wasm }) {
                    transformedMetadataDependencyProvider?.get(kotlinSourceSet)
                        ?: objects.fileCollection()
                } else {
                    objects.fileCollection()
                }
            }

        register(kotlinSourceSet.name) {
            this.dependentSourceSetIds.addAll(dependentSourceSetIds)
            this.sourceDirectories.from(extantSourceDirectories)
            this.sourceDirectoriesOfDependents.from(sourceDirectoriesOfDependents)
            this.primaryCompilations.addAll(primaryCompilations)
            this.allAssociatedCompilations.addAll(allAssociatedCompilations)
            this.transformedMetadataDependencies.from(transformedMetadataDependencies)
        }
    }


    /**
     * Return a list containing _all_ source sets that this source set depends on,
     * searching recursively.
     *
     * For example, `linuxX64` depends on `linuxMain`, `nativeMain`, and `commonMain`.
     * [KotlinSourceSet.dependsOn] only returns the direct dependency: `linuxMain`.
     *
     * @see KotlinSourceSet.dependsOn
     */
    private fun KotlinSourceSet.allDependsOnSourceSets(): Provider<List<KotlinSourceSet>> {

        tailrec fun allDependsOn(
            queue: Set<KotlinSourceSet>,
            allDependents: List<KotlinSourceSet> = emptyList(),
        ): List<KotlinSourceSet> {
            val next = queue.firstOrNull() ?: return allDependents
            return allDependsOn(
                queue = (queue - next) union next.dependsOn,
                allDependents = allDependents + next,
            )
        }

        return providers.provider {
            allDependsOn(queue = dependsOn.toSet())
        }
    }
}


/** Try and get [KotlinProjectExtension], or `null` if it's not present. */
private fun Project.findKotlinExtension(): KotlinProjectExtension? =
    findExtensionLenient<KotlinProjectExtension>("kotlin")


/** Try and get [AndroidComponentsExtension], or `null` if it's not present. */
private fun Project.findAndroidComponentExtension(): AndroidComponentsExtension<*, *, *>? =
    findExtensionLenient<AndroidComponentsExtension<*, *, *>>("androidComponents")


/**
 * Store details about a [Variant].
 *
 * @param[name] [Variant.name].
 * @param[hasPublishedComponent] `true` if any component of the variant is 'published',
 * i.e. it is an instance of [Variant].
 */
private data class AndroidVariantInfo(
    val name: String,
    val hasPublishedComponent: Boolean,
)

/**
 * Collect [AndroidVariantInfo]s of the Android [Variant]s in this Android project.
 *
 * We store the collected data in a custom class to aid with Configuration Cache compatibility.
 *
 * This function must only be called when AGP is applied
 * (otherwise [findAndroidComponentExtension] will return `null`),
 * i.e. inside a `withPlugin(...) {}` block.
 *
 * ## How to determine publishability of AGP Variants
 *
 * There are several Android Gradle plugins.
 * Each AGP has a specific associated [Variant]:
 * - `com.android.application` - [com.android.build.api.variant.ApplicationVariant]
 * - `com.android.library` - [com.android.build.api.variant.DynamicFeatureVariant]
 * - `com.android.test` - [com.android.build.api.variant.LibraryVariant]
 * - `com.android.dynamic-feature` - [com.android.build.api.variant.TestVariant]
 *
 * A [Variant] is 'published' (or otherwise shared with other projects).
 * Note that a [Variant] might have [nestedComponents][Variant.nestedComponents].
 * If any of these [com.android.build.api.variant.Component]s are [Variant]s,
 * then the [Variant] itself should be considered 'publishable'.
 *
 * If a [KotlinSourceSet] has an associated [Variant],
 * it should therefore be documented by Dokka by default.
 *
 * ### Associating Variants with Compilations with SourceSets
 *
 * So, how can we associate a [KotlinSourceSet] with a [Variant]?
 *
 * Fortunately, Dokka already knows about the [KotlinCompilation]s associated with a specific [KotlinSourceSet].
 *
 * So, for each [KotlinCompilation], find a [Variant] with the same name,
 * i.e. [KotlinCompilation.getName] is the same as [Variant.name].
 *
 * Next, determine if the [Variant] associated with a [KotlinCompilation] is 'publishable' by
 * checking if it _or_ any of its [nestedComponents][Variant.nestedComponents]
 * are 'publishable' (i.e. is an instance of [Variant]).
 * (We can we use [Variant.components] to check both the [Variant] and its `nestedComponents` the same time.)
 */
private fun collectAndroidVariants(
    project: Project,
    androidVariants: SetProperty<AndroidVariantInfo>,
) {
    val androidComponents = project.findAndroidComponentExtension()

    androidComponents?.onVariants { variant ->
        val hasPublishedComponent =
            variant.components.any { component ->
                // a Variant is a subtype of a Component that is shared with consumers,
                // so Dokka should consider it 'publishable'
                component is Variant
            }

        androidVariants.add(
            AndroidVariantInfo(
                name = variant.name,
                hasPublishedComponent = hasPublishedComponent,
            )
        )
    }
}
