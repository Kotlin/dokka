package org.jetbrains.dokka.gradle.services

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toJsonString
import org.jetbrains.dokka.utilities.LoggingLevel
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class DokkaExecutorService @Inject constructor(
    private val executor: ExecOperations,
) : BuildService<DokkaExecutorService.Params> {

    interface Params : BuildServiceParameters {
        val dokkaGeneratorClass: Property<String>
        val defaultLoggingLevel: Property<LoggingLevel>
    }

    private val dokkaGeneratorClass = parameters.dokkaGeneratorClass.get()
    private val defaultLoggingLevel = parameters.defaultLoggingLevel.get()

    fun generate(
        dokkaConfiguration: DokkaConfiguration,
        runtimeClasspath: Collection<File>,
        loggingLevel: LoggingLevel = defaultLoggingLevel,
    ) {
        val dokkaConfigurationFile = Files.createTempFile("dokka", ".json").toFile()
        dokkaConfigurationFile.writeText(dokkaConfiguration.toJsonString())

        executor.javaexec {
            mainClass.set(dokkaGeneratorClass)
            classpath(runtimeClasspath)
            isIgnoreExitValue = true
            args(
                dokkaConfigurationFile.invariantSeparatorsPath,
                loggingLevel.name,
            )
        }
    }


}
