package org.jetbrains.dokka.dokkatoo.workers

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.LoggerAdapter
import java.io.File
import java.time.Duration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on its classpath, as well as any Dokka plugins
 * that are used to generate the Dokka files. Transitive dependencies are also required.
 */
@DokkatooInternalApi
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

  @DokkatooInternalApi
  interface Parameters : WorkParameters {
    val dokkaParameters: Property<DokkaConfiguration>
    val logFile: RegularFileProperty
  }

  override fun execute() {
    val dokkaParameters = parameters.dokkaParameters.get()

    prepareOutputDir(dokkaParameters)

    executeDokkaGenerator(
      parameters.logFile.get().asFile,
      dokkaParameters,
    )
  }

  private fun prepareOutputDir(dokkaParameters: DokkaConfiguration) {
    // Dokka Generator doesn't clean up old files, so we need to manually clean the output directory
    dokkaParameters.outputDir.deleteRecursively()
    dokkaParameters.outputDir.mkdirs()

    // workaround until https://github.com/Kotlin/dokka/pull/2867 is released
    dokkaParameters.modules.forEach { module ->
      val moduleDir = dokkaParameters.outputDir.resolve(module.relativePathToOutputDirectory)
      moduleDir.mkdirs()
    }
  }

  private fun executeDokkaGenerator(
    logFile: File,
    dokkaParameters: DokkaConfiguration
  ) {
    LoggerAdapter(logFile).use { logger ->
      logger.progress("Executing DokkaGeneratorWorker with dokkaParameters: $dokkaParameters")

      val generator = DokkaGenerator(dokkaParameters, logger)

      val duration = measureTime { generator.generate() }

      logger.info("DokkaGeneratorWorker completed in $duration")
    }
  }

  @DokkatooInternalApi
  companion object {
    // can't use kotlin.Duration or kotlin.time.measureTime {} because
    // the implementation isn't stable across Kotlin versions
    private fun measureTime(block: () -> Unit): Duration =
      System.nanoTime().let { startTime ->
        block()
        Duration.ofNanos(System.nanoTime() - startTime)
      }
  }
}
