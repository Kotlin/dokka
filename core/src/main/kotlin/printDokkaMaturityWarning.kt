package org.jetbrains.dokka

import org.jetbrains.dokka.utilities.DokkaLogger

internal fun printDokkaMaturityWarning(logger: DokkaLogger) {
    logger.warn("dokka 1.4.* is an alpha project")
}
