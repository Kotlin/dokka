package org.jetbrains.dokka.maven

import org.apache.maven.plugin.logging.Log
import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.concurrent.atomic.AtomicInteger

class MavenDokkaLogger(val log: Log) : DokkaLogger {
    private val warningsCounter = AtomicInteger()
    private val errorsCounter = AtomicInteger()

    override var warningsCount: Int
        get() = warningsCounter.get()
        set(value) = warningsCounter.set(value)

    override var errorsCount: Int
        get() = errorsCounter.get()
        set(value) = errorsCounter.set(value)

    override fun debug(message: String) = log.debug(message)
    override fun info(message: String) = log.info(message)
    override fun progress(message: String) = log.info(message)
    override fun warn(message: String) = log.warn(message).also { warningsCounter.incrementAndGet() }
    override fun error(message: String) = log.error(message).also { errorsCounter.incrementAndGet() }
}
