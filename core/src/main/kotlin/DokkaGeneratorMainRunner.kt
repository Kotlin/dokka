package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import java.io.File

/**
 * Used by Dokka Gradle Plugin to generate Dokka documentation.
 *
 * There are two arguments:
 *
 * 1. A path to a JSON encoded [DokkaConfiguration] file.
 * 2. The minimum logger level ([DokkaGenerator.logger]).
 */
fun main(args: Array<String>) {
    val dokkaConfigurationPath = args[0]
    val logLevel = LoggingLevel.valueOf(args[1])

    val dokkaConfigurationFile = File(dokkaConfigurationPath)
    val dokkaConfiguration = DokkaConfigurationImpl(dokkaConfigurationFile.readText())

    val generator = DokkaGenerator(
        dokkaConfiguration,
        DokkaConsoleLogger(minLevel = logLevel)
    )

    generator.generate()
}
