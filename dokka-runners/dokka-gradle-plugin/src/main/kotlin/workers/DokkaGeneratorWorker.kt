/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.workers

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.LoggerAdapter
import org.jetbrains.dokka.gradle.internal.generateDocumentationViaDokkaBootstrap
import java.io.File
import java.time.Duration
import java.util.function.BiConsumer

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on its classpath, as well as any Dokka plugins
 * that are used to generate the Dokka files. Transitive dependencies are also required.
 */
@InternalDokkaGradlePluginApi
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

    @InternalDokkaGradlePluginApi
    interface Parameters : WorkParameters {
        val dokkaClasspath: ConfigurableFileCollection
        val dokkaParameters: Property<DokkaConfiguration>
        val logFile: RegularFileProperty

        /**
         * The [org.gradle.api.Task.getPath] of the task that invokes this worker.
         * Only used in log messages.
         */
        val taskPath: Property<String>
    }

    override fun execute() {
        val dokkaParameters = parameters.dokkaParameters.get()
        val classpath = parameters.dokkaClasspath.files

        prepareOutputDir(dokkaParameters)

        executeDokkaGenerator(
            parameters.logFile.get().asFile,
            dokkaParameters,
            classpath
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
        dokkaParameters: DokkaConfiguration,
        classpath: Set<File>
    ) {
        LoggerAdapter(
            logFile,
            logger,
            logTag = parameters.taskPath.get(),
        ).use { logger ->
            logger.progress("Executing DokkaGeneratorWorker with dokkaParameters: $dokkaParameters")

            // A custom class loader (DokkaBootstrap) from DGPv1 is used due to https://github.com/gradle/gradle/issues/34442
            // A shadowed stdlib from kotlin-analysis-* should be available
            val duration = measureTime {
                generateDocumentationViaDokkaBootstrap(
                    dokkaClasspath = classpath,
                    dokkaConfiguration = dokkaParameters,
                    logger = createProxyLogger(logger)
                )
            }

            logger.info("DokkaGeneratorWorker completed in $duration")
        }
    }

    private fun createProxyLogger(logger: LoggerAdapter): BiConsumer<String, String> =
        BiConsumer<String, String> { level, message ->
            // otherwise, map the Dokka log level (org.jetbrains.dokka.utilities.LoggingLevel)
            // to an equivalent Gradle log level
            when (level) {
                "debug" -> logger.debug(message)
                "info" -> logger.info(message)
                "progress" -> logger.progress(message)
                "warn" -> logger.warn(message)
                "error" -> logger.error(message)
            }
        }

    @InternalDokkaGradlePluginApi
    companion object {
        private val logger: Logger = Logging.getLogger(DokkaGeneratorWorker::class.java)

        // can't use kotlin.Duration or kotlin.time.measureTime {} because
        // the implementation isn't stable across Kotlin versions
        private fun measureTime(block: () -> Unit): Duration =
            System.nanoTime().let { startTime ->
                block()
                Duration.ofNanos(System.nanoTime() - startTime)
            }
    }
}
