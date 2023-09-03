/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.maven

import org.apache.maven.plugin.logging.Log
import org.jetbrains.dokka.utilities.DokkaLogger
import java.util.concurrent.atomic.AtomicInteger

public class MavenDokkaLogger(
    public val log: Log
) : DokkaLogger {
    private val warningsCounter = AtomicInteger()
    private val errorsCounter = AtomicInteger()

    override var warningsCount: Int
        get() = warningsCounter.get()
        set(value) = warningsCounter.set(value)

    override var errorsCount: Int
        get() = errorsCounter.get()
        set(value) = errorsCounter.set(value)

    override fun debug(message: String) {
        log.debug(message)
    }

    override fun info(message: String) {
        log.info(message)
    }

    override fun progress(message: String) {
        log.info(message)
    }

    override fun warn(message: String) {
        this.log.warn(message).also { warningsCounter.incrementAndGet() }
    }

    override fun error(message: String) {
        log.error(message).also { errorsCounter.incrementAndGet() }
    }
}
