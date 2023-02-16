package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.concurrent.atomic.AtomicInteger

import java.util.function.BiConsumer

/**
 * Accessed with reflection
 */
@Suppress("unused")
class DokkaBootstrapImpl : DokkaBootstrap {

    private lateinit var generator: DokkaGenerator

    override fun configure(serializedConfigurationJSON: String, logger: DokkaLogger) {
        val configuration = DokkaConfigurationImpl(serializedConfigurationJSON)
        generator = DokkaGenerator(configuration, logger)
    }

    override fun generate() = generator.generate()
}
