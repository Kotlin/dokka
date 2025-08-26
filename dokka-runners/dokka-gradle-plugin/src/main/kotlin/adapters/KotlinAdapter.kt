/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.adapters

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
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
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import org.jetbrains.dokka.gradle.internal.*
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
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
 * It is not a standalone plugin, it requires [org.jetbrains.dokka.gradle.DokkaBasePlugin] is also applied.
 */
@InternalDokkaGradlePluginApi
abstract class KotlinAdapter @Inject constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(project: Project) {
        logger.info("Applying $dkaName to ${project.path}")

        project.plugins.withType<DokkaBasePlugin>().configureEach {
            project.pluginManager.apply {
                withPlugin(PluginId.KotlinAndroid) { exec(project) }
                withPlugin(PluginId.KotlinJs) { exec(project) }
                withPlugin(PluginId.KotlinJvm) { exec(project) }
                withPlugin(PluginId.KotlinMultiplatform) { exec(project) }
            }
        }
    }

    private fun exec(project: Project) {
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
        val kssClasspath = determineClasspath(details)

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
        return details.allCompilations.map { compilations: List<KotlinCompilationDetails> ->
            val allPlatforms = compilations
                // Exclude metadata compilations: they are always KotlinPlatform.Common, which isn't relevant here.
                // Dokka only cares about the compilable KMP targets of a KotlinSourceSet.
                .filter { !it.isMetadata }
                .map { it.kotlinPlatform }
                .distinct()

            val singlePlatform = allPlatforms.singleOrNull()

            if (singlePlatform == null) {
                logger.info(
                    "[$projectPath] Dokka could not determine KotlinPlatform for ${details.name} from targets ${compilations.map { it.target }}. " +
                            "Dokka will assume this is a ${KotlinPlatform.Common} source set. " +
                            "(All platforms: $allPlatforms)"
                )
                KotlinPlatform.Common
            } else {
                singlePlatform
            }
        }
    }

    private fun determineClasspath(
        details: KotlinSourceSetDetails
    ): Provider<FileCollection> {
        return details.primaryCompilations.map { compilations: List<KotlinCompilationDetails> ->
            val classpath = objects.fileCollection()

            if (compilations.isNotEmpty()) {
                compilations.fold(classpath) { acc, compilation ->
                    acc.from(compilation.compilationClasspath)
                }
            } else {
                classpath
                    .from(details.sourceDirectories)
                    .from(details.sourceDirectoriesOfDependents)
            }
        }
    }

    @InternalDokkaGradlePluginApi
    companion object {
        private val dkaName: String = KotlinAdapter::class.simpleName!!

        private val logger = Logging.getLogger(KotlinAdapter::class.java)

        /** Try and get [KotlinProjectExtension], or `null` if it's not present. */
        private fun Project.findKotlinExtension(): KotlinProjectExtension? {
            val kotlinExtension =
                try {
                    extensions.findByType<KotlinProjectExtension>()
                    // fallback to trying to get the JVM extension
                    // (not sure why I did this... maybe to be compatible with really old versions?)
                        ?: extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
                } catch (e: Throwable) {
                    when (e) {
                        is TypeNotPresentException,
                        is ClassNotFoundException,
                        is NoClassDefFoundError -> {
                            logger.info("$dkaName failed to find KotlinExtension ${e::class} ${e.message}")
                            null
                        }

                        else -> throw e
                    }
                }

            if (kotlinExtension == null) {
                if (project.extensions.findByName("kotlin") != null) {
                    // uh oh - the Kotlin extension is present but findKotlinExtension() failed.
                    // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
                    logger.warn {
                        val allPlugins =
                            project.plugins.joinToString { it::class.qualifiedName ?: "${it::class}" }
                        val allExtensions =
                            project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

                        /* language=TEXT */
                        """
                        |$dkaName failed to get KotlinProjectExtension in ${project.path}
                        |  Applied plugins: $allPlugins
                        |  Available extensions: $allExtensions
                        """.trimMargin()
                    }
                }
            }

            return kotlinExtension
        }

        /** Get the version of the Kotlin Gradle Plugin currently used to compile the project. */
        // Must be lazy, else tests fail (because the KGP plugin isn't accessible)
        internal val currentKotlinToolingVersion: KotlinToolingVersion by lazy {
            val kgpVersion = getKotlinPluginVersion(logger)
            KotlinToolingVersion(kgpVersion)
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
    private val androidComponentsInfo: Provider<Set<AgpVariantInfo>> = getAgpVariantInfo(project)

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

    private data class AgpVariantInfo(
        val name: String,
        val hasPublishedComponent: Boolean,
    )

    private fun getAgpVariantInfo(
        project: Project,
    ): Provider<Set<AgpVariantInfo>> {

        val androidVariants = objects.setProperty(AgpVariantInfo::class)

        fun collectAndroidVariants() {
            val androidComponents = project.findAndroidComponentExtension()

            androidComponents?.onVariants { variant ->
                androidVariants.add(
                    AgpVariantInfo(
                        name = variant.name,
                        hasPublishedComponent = variant.components.any { it is com.android.build.api.variant.Variant },
                    )
                )
            }
        }

        project.pluginManager.apply {
            withPlugin(PluginId.AndroidBase) { collectAndroidVariants() }
            withPlugin(PluginId.AndroidApplication) { collectAndroidVariants() }
            withPlugin(PluginId.AndroidLibrary) { collectAndroidVariants() }
        }
        return androidVariants

//            try {
//                project.extensions.findByType(AndroidComponentsExtension::class)
//            } catch (ex: ClassNotFoundException) {
//                KotlinCompilationDetailsBuilder.Companion.logger.info("Unable to find AndroidComponentsExtension in project $project", ex)
//                null
//            }
//        if (androidComponents == null) {
//            KotlinCompilationDetailsBuilder.Companion.logger.warn("AndroidComponentsExtension not available in project $project")
//            return providers.provider { false }
//        } else {
//            val agpVariants = objects.listProperty(AgpVariantInfo::class)
//
//            androidComponents.onVariants { v ->
//                println("[DOKKA KotlinAdapter] ${compilation.name} checking variant $v, ${v.name}, ${v.components.map { "${it.name}=${it::class}" }}")
//                agpVariants.add(
//                    AgpVariantInfo(
//                        name = v.name,
//                        hasPublishedComponent =
//                            v.components.any { it is com.android.build.api.variant.Variant },
//                    )
//                )
//            }
//
//            return agpVariants.map { agpVariants ->
//                agpVariants
//                    .singleOrNull { it.name == compilation.name }
//                    ?.hasPublishedComponent
//                    ?: false
//            }
//        }
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
            isMetadata = compilation is KotlinMetadataTarget,
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

            // Use string-based comparison, not the actual classes, because AGP has deprecated and
            // moved the Library/Application classes to a different package.
            // Using strings is more widely compatible.
            is KotlinJvmAndroidCompilation -> {
                isJvmAndroidPublished(this)
            }

            else ->
                providers.provider { name == MAIN_COMPILATION_NAME }
        }
    }

    private fun isJvmAndroidPublished(compilation: KotlinJvmAndroidCompilation): Provider<Boolean> {
        println("[DOKKA KotlinAdapter] Checking if ${compilation.name} is publishable... (currentKotlinToolingVersion:${currentKotlinToolingVersion})")

//        if (currentKotlinToolingVersion < KotlinToolingVersion("2.2.10")) { // TODO revert, I made it lower for easier manual testing
        if (currentKotlinToolingVersion < KotlinToolingVersion("2.1.10")) {
            val variantName = compilation.androidVariant.name
            return providers.provider {
                val x = "LibraryVariant" in variantName || "ApplicationVariant" in variantName
                println("[DOKKA KotlinAdapter] ${compilation.name} publishable:$x, variant:$variantName")
                x
            }
        } else {
            return androidComponentsInfo.map { components ->
                components.any { component ->
                    val x = component.name == compilation.name
                            && component.hasPublishedComponent
                    println("[DOKKA KotlinAdapter] ${compilation.name} publishable:$x, component:${component.name}")
                    x
                }
            }
        }
    }

    companion object {
        private val logger = Logging.getLogger(KotlinCompilationDetailsBuilder::class.java)
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
    abstract val allCompilations: ListProperty<KotlinCompilationDetails>

    /**
     * Estimate if this Kotlin source set contains 'published' (non-test) sources.
     *
     * @see KotlinCompilationDetails.publishedCompilation
     */
    fun isPublishedSourceSet(): Provider<Boolean> =
        allCompilations.map { values ->
            values.any { it.publishedCompilation.get() }
        }

    override fun getName(): String = named
}


/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinSourceSetDetailsBuilder(
    private val sourceSetScopeDefault: Provider<String>,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    /** [Project.getPath]. Used for logging. */
    private val projectPath: String,
) {

    private val logger = Logging.getLogger(KotlinSourceSetDetails::class.java)

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

        val primaryCompilations = allKotlinCompilationDetails.map { primaryCompilations ->
            primaryCompilations.filter { compilation ->
                kotlinSourceSet.name in compilation.primarySourceSetNames
            }
        }

        val allCompilations = allKotlinCompilationDetails.map { allCompilations ->
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

        val sourceDirectoriesOfDependents = providers.provider {
            kotlinSourceSet
                .allDependentSourceSets()
                .fold(objects.fileCollection()) { acc, sourceSet ->
                    acc.from(sourceSet.kotlin.sourceDirectories)
                }
        }

        register(kotlinSourceSet.name) {
            this.dependentSourceSetIds.addAll(dependentSourceSetIds)
            this.sourceDirectories.from(extantSourceDirectories)
            this.sourceDirectoriesOfDependents.from(sourceDirectoriesOfDependents)
            this.primaryCompilations.addAll(primaryCompilations)
            this.allCompilations.addAll(allCompilations)
        }
    }


    /**
     * Return a list containing _all_ source sets that this source set depends on,
     * searching recursively.
     *
     * @see KotlinSourceSet.dependsOn
     */
    private tailrec fun KotlinSourceSet.allDependentSourceSets(
        queue: Set<KotlinSourceSet> = dependsOn.toSet(),
        allDependents: List<KotlinSourceSet> = emptyList(),
    ): List<KotlinSourceSet> {
        val next = queue.firstOrNull() ?: return allDependents
        return next.allDependentSourceSets(
            queue = (queue - next) union next.dependsOn,
            allDependents = allDependents + next,
        )
    }
}


private typealias AndroidComponentsExtension = com.android.build.api.variant.AndroidComponentsExtension<*, *, *>


/** Try and get [KotlinProjectExtension], or `null` if it's not present. */
private fun Project.findAndroidComponentExtension(): AndroidComponentsExtension? {
    val androidComponentsExtensionName = "androidComponents"
    val androidComponentsExtension =
        try {
            val candidate = extensions.findByName(androidComponentsExtensionName)
            candidate as? AndroidComponentsExtension
        } catch (e: Throwable) {
            when (e) {
                is TypeNotPresentException,
                is ClassNotFoundException,
                is NoClassDefFoundError -> {
                    logger.info("Dokka Gradle plugin failed to find AndroidComponentsExtension ${e::class} ${e.message}")
                    null
                }

                else -> throw e
            }
        }

    if (androidComponentsExtension == null) {
        if (project.extensions.findByName(androidComponentsExtensionName) != null) {
            // uh oh - extension is present but findAndroidComponentExtension() failed.
            // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
            logger.warn {
                val allPlugins =
                    project.plugins.joinToString { it::class.qualifiedName ?: "${it::class}" }
                val allExtensions =
                    project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

                /* language=TEXT */
                """
                |Dokka Gradle plugin failed to get AndroidComponentsExtension in ${project.path}
                |  Applied plugins: $allPlugins
                |  Available extensions: $allExtensions
                """.trimMargin()
            }
        }
    }

    return androidComponentsExtension
}
