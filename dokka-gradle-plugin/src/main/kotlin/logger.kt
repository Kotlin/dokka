package org.jetbrains.dokka.gradle

import org.gradle.api.logging.Logger
import org.jetbrains.dokka.DokkaLogger

class DokkaGradleLogger(val logger: Logger) : DokkaLogger {
    override fun error(message: String) {
        logger.error(message)
    }

    override fun info(message: String) {
        logger.info(message)
    }

    override fun warn(message: String) {
        logger.warn(message)
    }
}