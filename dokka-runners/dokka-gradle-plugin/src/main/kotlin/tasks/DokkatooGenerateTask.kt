package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.builders.DokkaParametersBuilder
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.workers.ClassLoaderIsolation
import dev.adamko.dokkatoo.workers.DokkaGeneratorWorker
import dev.adamko.dokkatoo.workers.ProcessIsolation
import dev.adamko.dokkatoo.workers.WorkerIsolation
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toPrettyJsonString

/**
 * Base task for executing Dokka Generator, producing documentation.
 *
 * The Dokka Plugins added to the generator classpath determine the type of documentation generated.
 */
@CacheableTask
abstract class DokkatooGenerateTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  archives: ArchiveOperations,
  private val workers: WorkerExecutor,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask() {

  private val dokkaParametersBuilder = DokkaParametersBuilder(archives)

  /**
   * Directory containing the generation result. The content and structure depends on whether
   * the task generates a Dokka Module or a Dokka Publication.
   */
  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  /**
   * Classpath required to run Dokka Generator.
   *
   * Contains the Dokka Generator, Dokka plugins, and any transitive dependencies.
   */
  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:LocalState
  abstract val cacheDirectory: DirectoryProperty

  /** @see dev.adamko.dokkatoo.dokka.DokkaPublication.enabled */
  @get:Input
  abstract val publicationEnabled: Property<Boolean>

  @get:Nested
  val generator: DokkaGeneratorParametersSpec = objects.newInstance(pluginsConfiguration)

  /**
   * Control whether Dokkatoo launches Dokka Generator.
   *
   * Defaults to [dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation].
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation
   */
  @get:Nested
  abstract val workerIsolation: Property<WorkerIsolation>

  @get:Internal
  abstract val workerLogFile: RegularFileProperty

  /**
   * The [DokkaConfiguration] by Dokka Generator can be saved to a file for debugging purposes.
   * To disable this behaviour set this property to `null`.
   */
  @DokkatooInternalApi
  @get:Internal
  abstract val dokkaConfigurationJsonFile: RegularFileProperty

  @DokkatooInternalApi
  enum class GeneratorMode {
    Module,
    Publication,
  }

  @DokkatooInternalApi
  protected fun generateDocumentation(
    generationType: GeneratorMode,
    outputDirectory: File,
  ) {
    val dokkaConfiguration = createDokkaConfiguration(generationType, outputDirectory)

    logger.info("dokkaConfiguration: $dokkaConfiguration")
    verifyDokkaConfiguration(dokkaConfiguration)
    dumpDokkaConfigurationJson(dokkaConfiguration)

    logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.asPath}")

    val isolation = workerIsolation.get()
    logger.info("[$path] running with workerIsolation $isolation")
    val workQueue = when (isolation) {
      is ClassLoaderIsolation ->
        workers.classLoaderIsolation {
          classpath.from(runtimeClasspath)
        }

      is ProcessIsolation     ->
        workers.processIsolation {
          classpath.from(runtimeClasspath)
          forkOptions {
            isolation.defaultCharacterEncoding.orNull?.let(this::setDefaultCharacterEncoding)
            isolation.debug.orNull?.let(this::setDebug)
            isolation.enableAssertions.orNull?.let(this::setEnableAssertions)
            isolation.maxHeapSize.orNull?.let(this::setMaxHeapSize)
            isolation.minHeapSize.orNull?.let(this::setMinHeapSize)
            isolation.jvmArgs.orNull?.let(this::setJvmArgs)
            isolation.systemProperties.orNull?.let(this::systemProperties)
          }
        }
    }

    workQueue.submit(DokkaGeneratorWorker::class) {
      this.dokkaParameters.set(dokkaConfiguration)
      this.logFile.set(workerLogFile)
    }
  }

  /**
   * Run some helper checks to log warnings if the [DokkaConfiguration] looks misconfigured.
   */
  private fun verifyDokkaConfiguration(dokkaConfiguration: DokkaConfiguration) {
    val modulesWithDuplicatePaths = dokkaConfiguration.modules
      .groupBy { it.relativePathToOutputDirectory.toString() }
      .filterValues { it.size > 1 }

    if (modulesWithDuplicatePaths.isNotEmpty()) {
      val modulePaths = modulesWithDuplicatePaths.entries
        .map { (path, modules) ->
          "${modules.joinToString { "'${it.name}'" }} have modulePath '$path'"
        }
        .sorted()
        .joinToString("\n") { "  - $it" }
      logger.warn(
        """
          |[$path] Duplicate `modulePath`s in Dokka Generator parameters.
          |  modulePaths must be distinct for each module, as they are used to determine the output
          |  directory. Duplicates mean Dokka Generator may overwrite one module with another.
          |$modulePaths
          |
        """.trimMargin()
      )
    }
  }

  /**
   * Dump the [DokkaConfiguration] JSON to a file ([dokkaConfigurationJsonFile]) for debugging
   * purposes.
   */
  private fun dumpDokkaConfigurationJson(
    dokkaConfiguration: DokkaConfiguration,
  ) {
    val destFile = dokkaConfigurationJsonFile.asFile.orNull ?: return
    destFile.parentFile.mkdirs()
    destFile.createNewFile()

    val compactJson = dokkaConfiguration.toPrettyJsonString()
    val json = jsonMapper.decodeFromString(JsonElement.serializer(), compactJson)
    val prettyJson = jsonMapper.encodeToString(JsonElement.serializer(), json)

    destFile.writeText(prettyJson)

    logger.info("[$path] Dokka Generator configuration JSON: ${destFile.toURI()}")
  }

  private fun createDokkaConfiguration(
    generationType: GeneratorMode,
    outputDirectory: File,
  ): DokkaConfiguration {

    val delayTemplateSubstitution = when (generationType) {
      GeneratorMode.Module      -> true
      GeneratorMode.Publication -> false
    }

    val moduleOutputDirectories = generator.moduleOutputDirectories.toList()
    logger.info("[$path] got ${moduleOutputDirectories.size} moduleOutputDirectories: $moduleOutputDirectories")

    return dokkaParametersBuilder.build(
      spec = generator,
      delayTemplateSubstitution = delayTemplateSubstitution,
      outputDirectory = outputDirectory,
      moduleDescriptorDirs = moduleOutputDirectories,
      cacheDirectory = cacheDirectory.asFile.orNull,
    )
  }


  //region Deprecated Properties
  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     debug = true
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.debug
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerDebugEnabled: Property<Boolean>

  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     minHeapSize = "512m"
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.minHeapSize
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerMinHeapSize: Property<String>

  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     maxHeapSize = "1g"
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.maxHeapSize
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerMaxHeapSize: Property<String>

  /**
   * Please move worker options:
   *
   * ```kotlin
   * // build.gradle.kts
   *
   * dokkatoo {
   *   dokkaGeneratorIsolation = ProcessIsolation {
   *     jvmArgs = listOf(
   *       "-XX:+UseStringDeduplication",
   *     )
   *   }
   * }
   * ```
   *
   * Worker options were moved to allow for configuring worker isolation.
   *
   * @see dev.adamko.dokkatoo.DokkatooExtension.dokkaGeneratorIsolation
   * @see dev.adamko.dokkatoo.workers.ProcessIsolation.jvmArgs
   */
  @get:Internal
  @Deprecated("Please move worker options to `DokkatooExtension.dokkaGeneratorIsolation`. Worker options were moved to allow for configuring worker isolation")
  abstract val workerJvmArgs: ListProperty<String>

  @Deprecated("Removed - Module and Publication generation have been moved to specific subtasks")
  @Suppress("unused")
  enum class GenerationType {
    MODULE,
    PUBLICATION,
  }

  /**
   * Deprecated - instead use specific subtasks for Module/Publication generation.
   *
   * @see DokkatooGenerateModuleTask
   * @see DokkatooGeneratePublicationTask
   */
  @get:Internal
  @Deprecated("Created specific Module/Publication subclasses")
  @Suppress("DEPRECATION", "unused")
  abstract val generationType: Property<GenerationType>
  //endregion
}
