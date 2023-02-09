package org.jetbrains.dokka.gradle.workers

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.gradle.internal.LoggerAdapter
import java.net.URLClassLoader

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on the classpath.
 */
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

    private val logger = LoggerAdapter(DokkaGeneratorWorker::class)

    interface Parameters : WorkParameters {
        val dokkaConfiguration: Property<DokkaConfigurationImpl>
    }

    override fun execute() {
        println("Current classloader")
        (this.javaClass.classLoader as URLClassLoader).urLs.forEach { println(it) }
        val dokkaConfiguration = parameters.dokkaConfiguration.get()

        logger.progress("Run ${dokkaConfiguration.moduleName} ${if(dokkaConfiguration.delayTemplateSubstitution) "Partial" else "MultiModule"} task on ${ ProcessHandle.current().pid()}" )
        val generator = DokkaGenerator(dokkaConfiguration, logger)
        generator.generate()
    }
}
