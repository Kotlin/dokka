package org.jetbrains.dokka.maven

import org.apache.maven.plugin.logging.Log
import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.concurrent.atomic.LongAdder

class MavenDokkaLogger(val log: Log) : DokkaLogger {
    private val warningsCounter = LongAdder()
    private val errorsCounter = LongAdder()

    override var warningsCount: Int = warningsCounter.toInt()
    override var errorsCount: Int = errorsCounter.toInt()

    override fun debug(message: String) = log.debug(message)
    override fun info(message: String) = log.info(message)
    override fun progress(message: String) = log.info(message)
    override fun warn(message: String) = log.warn(message).also { warningsCounter.increment() }
    override fun error(message: String) = log.error(message).also { errorsCounter.increment() }
}
