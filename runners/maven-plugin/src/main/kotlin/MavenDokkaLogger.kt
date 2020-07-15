package org.jetbrains.dokka.maven

import org.apache.maven.plugin.logging.Log
import org.jetbrains.dokka.utilities.DokkaLogger

class MavenDokkaLogger(val log: Log) : DokkaLogger {
    override var warningsCount: Int = 0
    override var errorsCount: Int = 0

    override fun debug(message: String) = log.debug(message)
    override fun info(message: String) = log.info(message)
    override fun progress(message: String) = log.info(message)
    override fun warn(message: String) = log.warn(message).also { warningsCount++ }
    override fun error(message: String) = log.error(message).also { errorsCount++ }
}
