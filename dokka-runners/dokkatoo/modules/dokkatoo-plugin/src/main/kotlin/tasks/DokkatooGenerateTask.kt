package org.jetbrains.dokka.dokkatoo.tasks

import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin.Companion.jsonMapper
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import org.jetbrains.dokka.dokkatoo.dokka.parameters.builders.DokkaParametersBuilder
import org.jetbrains.dokka.dokkatoo.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.workers.DokkaGeneratorWorker
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.process.JavaForkOptions
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toPrettyJsonString

/**
 * Executes the Dokka Generator, and produces documentation.
 *
 * The type of documentation generated is determined by the supplied Dokka Plugins in [generator].
 */
@CacheableTask
abstract class DokkatooGenerateTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  private val workers: WorkerExecutor,

  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [org.jetbrains.dokka.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooTask() {

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

  /**
   * Generating a Dokka Module? Set this to [GenerationType.MODULE].
   *
   * Generating a Dokka Publication? [GenerationType.PUBLICATION].
   */
  @get:Input
  abstract val generationType: Property<GenerationType>

  /** @see org.jetbrains.dokka.dokkatoo.dokka.DokkaPublication.enabled */
  @get:Input
  abstract val publicationEnabled: Property<Boolean>

  @get:Nested
  val generator: DokkaGeneratorParametersSpec = objects.newInstance(pluginsConfiguration)

  /** @see JavaForkOptions.getDebug */
  @get:Input
  abstract val workerDebugEnabled: Property<Boolean>
  /** @see JavaForkOptions.getMinHeapSize */
  @get:Input
  @get:Optional
  abstract val workerMinHeapSize: Property<String>
  /** @see JavaForkOptions.getMaxHeapSize */
  @get:Input
  @get:Optional
  abstract val workerMaxHeapSize: Property<String>
  /** @see JavaForkOptions.jvmArgs */
  @get:Input
  abstract val workerJvmArgs: ListProperty<String>
  @get:Internal
  abstract val workerLogFile: RegularFileProperty

  /**
   * The [DokkaConfiguration] by Dokka Generator can be saved to a file for debugging purposes.
   * To disable this behaviour set this property to `null`.
   */
  @DokkatooInternalApi
  @get:Internal
  abstract val dokkaConfigurationJsonFile: RegularFileProperty

  enum class GenerationType {
    MODULE,
    PUBLICATION,
  }

  @TaskAction
  internal fun generateDocumentation() {
    val dokkaConfiguration = createDokkaConfiguration()
    logger.info("dokkaConfiguration: $dokkaConfiguration")
    dumpDokkaConfigurationJson(dokkaConfiguration)

    logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.asPath}")

    val workQueue = workers.processIsolation {
      classpath.from(runtimeClasspath)
      forkOptions {
        defaultCharacterEncoding = "UTF-8"
        minHeapSize = workerMinHeapSize.orNull
        maxHeapSize = workerMaxHeapSize.orNull
        enableAssertions = true
        debug = workerDebugEnabled.get()
        jvmArgs = workerJvmArgs.get()
      }
    }

    workQueue.submit(DokkaGeneratorWorker::class) {
      this.dokkaParameters.set(dokkaConfiguration)
      this.logFile.set(workerLogFile)
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

  private fun createDokkaConfiguration(): DokkaConfiguration {
    val outputDirectory = outputDirectory.get().asFile

    val delayTemplateSubstitution = when (generationType.orNull) {
      GenerationType.MODULE      -> true
      GenerationType.PUBLICATION -> false
      null                       -> error("missing GenerationType")
    }

    val dokkaModuleDescriptors = dokkaModuleDescriptors()

    return DokkaParametersBuilder.build(
      spec = generator,
      delayTemplateSubstitution = delayTemplateSubstitution,
      outputDirectory = outputDirectory,
      modules = dokkaModuleDescriptors,
      cacheDirectory = cacheDirectory.asFile.orNull,
    )
  }

  private fun dokkaModuleDescriptors(): List<DokkaModuleDescriptionKxs> {
    return generator.dokkaModuleFiles.asFileTree
      .matching { include("**/module_descriptor.json") }
      .files.map { file ->
        try {
          val fileContent = file.readText()
          jsonMapper.decodeFromString(
            DokkaModuleDescriptionKxs.serializer(),
            fileContent,
          )
        } catch (ex: Exception) {
          throw IOException("Could not parse DokkaModuleDescriptionKxs from $file", ex)
        }
      }
  }
}
