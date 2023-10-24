package org.jetbrains.dokka.dokkatoo.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin
import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.adapters.DokkatooKotlinAdapter.Companion.currentKotlinToolingVersion
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.dokkatoo.dokka.parameters.KotlinPlatform
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.not
import java.io.File
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

/**
 * The [DokkatooKotlinAdapter] plugin will automatically register Kotlin source sets as Dokka source sets.
 *
 * This is not a standalone plugin, it requires [org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin] is also applied.
 */
@DokkatooInternalApi
abstract class DokkatooKotlinAdapter @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    logger.info("applied DokkatooKotlinAdapter to ${project.path}")

    project.plugins.withType<DokkatooBasePlugin>().configureEach {
      project.pluginManager.apply {
        withPlugin("org.jetbrains.kotlin.android") { exec(project) }
        withPlugin("org.jetbrains.kotlin.js") { exec(project) }
        withPlugin("org.jetbrains.kotlin.jvm") { exec(project) }
        withPlugin("org.jetbrains.kotlin.multiplatform") { exec(project) }
      }
    }
  }

  private fun exec(project: Project) {
    val kotlinExtension = project.extensions.findKotlinExtension() ?: run {
      logger.info("could not find Kotlin Extension")
      return
    }
    logger.info("Configuring Dokkatoo in Gradle Kotlin Project ${project.path}")

    val dokkatooExtension = project.extensions.getByType<DokkatooExtension>()

    // first fetch the relevant properties of all KotlinCompilations
    val compilationDetailsBuilder = KotlinCompilationDetailsBuilder(
      providers = providers,
      objects = objects,
      konanHome = dokkatooExtension.konanHome.asFile,
    )
    val allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails> =
      compilationDetailsBuilder.createCompilationDetails(
        kotlinProjectExtension = kotlinExtension,
      )

    // second, fetch the relevant properties of the Kotlin source sets
    val sourceSetDetailsBuilder = KotlinSourceSetDetailsBuilder(
      providers = providers,
      objects = objects,
      sourceSetScopeDefault = dokkatooExtension.sourceSetScopeDefault,
      projectPath = project.path,
    )
    val sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails> =
      sourceSetDetailsBuilder.createSourceSetDetails(
        kotlinSourceSets = kotlinExtension.sourceSets,
        allKotlinCompilationDetails = allKotlinCompilationDetails,
      )

    // for each Kotlin source set, register a Dokkatoo source set
    registerDokkatooSourceSets(
      dokkatooExtension = dokkatooExtension,
      sourceSetDetails = sourceSetDetails,
    )
  }

  /** Register a [DokkaSourceSetSpec] for each element in [sourceSetDetails] */
  private fun registerDokkatooSourceSets(
    dokkatooExtension: DokkatooExtension,
    sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails>,
  ) {
    // proactively use 'all' so source sets will be available in users' build files if they use `named("...")`
    sourceSetDetails.all details@{
      dokkatooExtension.dokkatooSourceSets.register(details = this@details)
    }
  }

  /** Register a single [DokkaSourceSetSpec] for [details] */
  private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.register(
    details: KotlinSourceSetDetails
  ) {
    val kssPlatform = details.compilations.map { values: List<KotlinCompilationDetails> ->
      values.map { it.kotlinPlatform }
        .distinct()
        .singleOrNull() ?: KotlinPlatform.Common
    }

    val kssClasspath = determineClasspath(details)

    register(details.name) dss@{
      suppress.set(!details.isPublishedSourceSet())
      sourceRoots.from(details.sourceDirectories)
      classpath.from(kssClasspath)
      analysisPlatform.set(kssPlatform)
      dependentSourceSets.addAllLater(details.dependentSourceSetIds)
    }
  }

  private fun determineClasspath(
    details: KotlinSourceSetDetails
  ): Provider<FileCollection> {
    return details.compilations.map { compilations: List<KotlinCompilationDetails> ->
      val classpath = objects.fileCollection()

      if (compilations.isNotEmpty()) {
        compilations.fold(classpath) { acc, compilation ->
          acc.from(compilation.compilationClasspath)
          // can't use compileDependencyFiles, it causes weird dependency resolution errors in Android projects
          //acc.from(providers.provider { compilation.compileDependencyFiles })
        }
      } else {
        classpath
          .from(details.sourceDirectories)
          .from(details.sourceDirectoriesOfDependents)
      }
    }
  }

  @DokkatooInternalApi
  companion object {
    private val logger = Logging.getLogger(DokkatooKotlinAdapter::class.java)

    /** Try and get [KotlinProjectExtension], or `null` if it's not present */
    private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
      try {
        findByType()
        // fallback to trying to get the JVM extension
        // (not sure why I did this... maybe to be compatible with really old versions?)
          ?: findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
      } catch (e: Throwable) {
        when (e) {
          is TypeNotPresentException,
          is ClassNotFoundException,
          is NoClassDefFoundError -> null

          else                    -> throw e
        }
      }

    /** Get the version of the Kotlin Gradle Plugin currently used to compile the project */
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
@DokkatooInternalApi
private data class KotlinCompilationDetails(
  val target: String,
  val kotlinPlatform: KotlinPlatform,
  val allKotlinSourceSetsNames: Set<String>,
  val publishedCompilation: Boolean,
  val dependentSourceSetNames: Set<String>,
  val compilationClasspath: FileCollection,
  val defaultSourceSetName: String,
)

/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinCompilationDetailsBuilder(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  private val konanHome: Provider<File>,
) {

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

  /** Create a single [KotlinCompilationDetails] for [compilation] */
  private fun createCompilationDetails(
    compilation: KotlinCompilation<*>,
  ): KotlinCompilationDetails {
    val allKotlinSourceSetsNames =
      compilation.allKotlinSourceSets.map { it.name } + compilation.defaultSourceSet.name

    val dependentSourceSetNames =
      compilation.defaultSourceSet.dependsOn.map { it.name }

    val compilationClasspath: FileCollection =
      collectKotlinCompilationClasspath(compilation = compilation)

    return KotlinCompilationDetails(
      target = compilation.target.name,
      kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
      allKotlinSourceSetsNames = allKotlinSourceSetsNames.toSet(),
      publishedCompilation = compilation.isPublished(),
      dependentSourceSetNames = dependentSourceSetNames.toSet(),
      compilationClasspath = compilationClasspath,
      defaultSourceSetName = compilation.defaultSourceSet.name
    )
  }

  private fun KotlinProjectExtension.allKotlinCompilations(): Collection<KotlinCompilation<*>> =
    when (this) {
      is KotlinMultiplatformExtension   -> targets.flatMap { it.compilations }
      is KotlinSingleTargetExtension<*> -> target.compilations
      else                              -> emptyList() // shouldn't happen?
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

    // collect dependency files from 'regular' Kotlin compilations
    compilationClasspath.from(providers.provider { compilation.compileDependencyFiles })

    // apply workaround for Kotlin/Native, which will be fixed in Kotlin 2.0
    // (see KT-61559: K/N dependencies will be part of `compilation.compileDependencyFiles`)
    if (
      currentKotlinToolingVersion < KotlinToolingVersion("2.0.0")
      &&
      compilation is AbstractKotlinNativeCompilation
    ) {
      compilationClasspath.from(
        konanHome.map { konanHome ->
          kotlinNativeDependencies(konanHome, compilation.konanTarget)
        }
      )
    }

    return compilationClasspath
  }

  private fun kotlinNativeDependencies(konanHome: File, target: KonanTarget): FileCollection {
    val konanDistribution = KonanDistribution(konanHome)

    val dependencies = objects.fileCollection()

    dependencies.from(konanDistribution.stdlib)

    // Konan library files for a specific target
    dependencies.from(
      konanDistribution.platformLibsDir
        .resolve(target.name)
        .listFiles()
        .orEmpty()
        .filter { it.isDirectory || it.extension == "klib" }
    )

    return dependencies
  }

  companion object {

    /**
     * Determine if a [KotlinCompilation] is 'publishable', and so should be enabled by default
     * when creating a Dokka publication.
     *
     * Typically, 'main' compilations are publishable and 'test' compilations should be suppressed.
     * This can be overridden manually, though.
     *
     * @see DokkaSourceSetSpec.suppress
     */
    private fun KotlinCompilation<*>.isPublished(): Boolean {
      return when (this) {
        is KotlinMetadataCompilation<*> -> true

        is KotlinJvmAndroidCompilation  ->
          androidVariant is LibraryVariant || androidVariant is ApplicationVariant

        else                            ->
          name == MAIN_COMPILATION_NAME
      }
    }
  }
}


/**
 * Store the details of all [KotlinSourceSet]s in a configuration cache compatible way.
 *
 * @param[named] Should be [KotlinSourceSet.getName]
 */
@DokkatooInternalApi
private abstract class KotlinSourceSetDetails @Inject constructor(
  private val named: String,
) : Named {

  /** Direct source sets that this source set depends on */
  abstract val dependentSourceSetIds: SetProperty<DokkaSourceSetIdSpec>
  abstract val sourceDirectories: ConfigurableFileCollection
  /** _All_ source directories from any (recursively) dependant source set */
  abstract val sourceDirectoriesOfDependents: ConfigurableFileCollection
  /** The specific compilations used to build this source set */
  abstract val compilations: ListProperty<KotlinCompilationDetails>

  /** Estimate if this Kotlin source set contains 'published' sources */
  fun isPublishedSourceSet(): Provider<Boolean> =
    compilations.map { values ->
      values.any { it.publishedCompilation }
    }

  override fun getName(): String = named
}

/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinSourceSetDetailsBuilder(
  private val sourceSetScopeDefault: Provider<String>,
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
  /** Used for logging */
  private val projectPath: String,
) {

  private val logger = Logging.getLogger(KotlinSourceSetDetails::class.java)

  fun createSourceSetDetails(
    kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
  ): NamedDomainObjectContainer<KotlinSourceSetDetails> {

    val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails::class)

    kotlinSourceSets.configureEach kss@{
      sourceSetDetails.register(
        kotlinSourceSet = this,
        allKotlinCompilationDetails = allKotlinCompilationDetails,
      )
    }

    return sourceSetDetails
  }

  private fun NamedDomainObjectContainer<KotlinSourceSetDetails>.register(
    kotlinSourceSet: KotlinSourceSet,
    allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
  ) {

    // TODO: Needs to respect filters.
    //  We probably need to change from "sourceRoots" to support "sourceFiles"
    //  https://github.com/Kotlin/dokka/issues/1215
    val extantSourceDirectories = providers.provider {
      kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }
    }

    val compilations = allKotlinCompilationDetails.map { allCompilations ->
      allCompilations.filter { compilation ->
        kotlinSourceSet.name in compilation.allKotlinSourceSetsNames
      }
    }

    // determine the source sets IDs of _other_ source sets that _this_ source depends on.
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
      this.compilations.addAll(compilations)
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
