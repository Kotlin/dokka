package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.concurrent.atomic.LongAdder

import java.util.function.BiConsumer

/**
 * Accessed with reflection
 */
@Suppress("unused")
class DokkaBootstrapImpl : DokkaBootstrap {

    class DokkaProxyLogger(val consumer: BiConsumer<String, String>) : DokkaLogger {
        private val warningsCounter = LongAdder()
        private val errorsCounter = LongAdder()

        override var warningsCount: Int = warningsCounter.toInt()
        override var errorsCount: Int = errorsCounter.toInt()

        override fun debug(message: String) {
            consumer.accept("debug", message)
        }

        override fun info(message: String) {
            consumer.accept("info", message)
        }

        override fun progress(message: String) {
            consumer.accept("progress", message)
        }

        override fun warn(message: String) {
            consumer.accept("warn", message).also { warningsCounter.increment() }
        }

        override fun error(message: String) {
            consumer.accept("error", message).also { errorsCounter.increment() }
        }
    }

    private lateinit var generator: DokkaGenerator

    fun configure(logger: DokkaLogger, configuration: DokkaConfigurationImpl) {
        generator = DokkaGenerator(configuration, logger)
    }

    override fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>) = configure(
        DokkaProxyLogger(logger),
        DokkaConfigurationImpl(serializedConfigurationJSON)
    )

    override fun generate() = generator.generate()
}
