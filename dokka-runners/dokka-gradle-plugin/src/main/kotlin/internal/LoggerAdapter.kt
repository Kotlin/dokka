/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.logging.Logger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.jetbrains.dokka.utilities.LoggingLevel.*
import java.io.File
import java.io.Writer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Logs all Dokka messages to [logWriter],
 * and redirects all messages to [logger].
 *
 * @see org.jetbrains.dokka.DokkaGenerator
 */
// Gradle causes OOM errors when there is a lot of console output. Logging to file is a workaround.
// https://github.com/gradle/gradle/issues/23965
// https://github.com/gradle/gradle/issues/15621
internal class LoggerAdapter(
    outputFile: File,
    private val logger: Logger,
) : DokkaLogger, AutoCloseable {

    private val logWriter: Writer

    init {
        if (!outputFile.exists()) {
            outputFile.parentFile.mkdirs()
            outputFile.createNewFile()
        }

        logWriter = outputFile.bufferedWriter()
    }

    private val warningsCounter = AtomicInteger()
    private val errorsCounter = AtomicInteger()

    override var warningsCount: Int
        get() = warningsCounter.get()
        set(value) = warningsCounter.set(value)

    override var errorsCount: Int
        get() = errorsCounter.get()
        set(value) = errorsCounter.set(value)

    override fun debug(message: String) = log(DEBUG, message)
    override fun progress(message: String) = log(PROGRESS, message)
    override fun info(message: String) = log(INFO, message)

    override fun warn(message: String) {
        warningsCount++
        log(WARN, message)
    }

    override fun error(message: String) {
        errorsCount++
        log(ERROR, message)
    }

    @Synchronized
    private fun log(level: LoggingLevel, message: String) {
        when (level) {
            DEBUG -> logger.info(message.prependIndent())
            PROGRESS -> logger.info(message.prependIndent())
            INFO -> logger.info(message.prependIndent())
            WARN -> logger.warn(message.prependIndent())
            ERROR -> logger.warn(message.prependIndent())
        }
        logWriter.appendLine("[${level.name}] $message")
    }

    override fun close() {
        logWriter.close()
    }
}
