/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.jetbrains.dokka.utilities.LoggingLevel.*
import org.slf4j.Logger
import java.io.File
import java.io.Writer
import java.util.concurrent.atomic.AtomicInteger

/**
 * A logger for [org.jetbrains.dokka.DokkaGenerator].
 *
 * All messages will be written to [logWriter] and forwarded to a Gradle [logger] (at an appropriate log level).
 *
 * [org.jetbrains.dokka.DokkaGenerator] makes heavy use of coroutines and parallelization,
 * so use thread-safe practices when handling logging messages.
 *
 * @param logTag Prepend all [logger] messages with this tag.
 * @see org.jetbrains.dokka.DokkaGenerator
 */
// Gradle causes OOM errors when there is a lot of console output. Logging to file is a workaround.
// https://github.com/gradle/gradle/issues/23965
// https://github.com/gradle/gradle/issues/15621
internal class LoggerAdapter(
    outputFile: File,
    private val logger: Logger,
    private val logTag: String,
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
            PROGRESS,
            INFO -> logger.info("[$logTag] " + message.prependIndent().trimStart())

            DEBUG -> logger.debug("[$logTag] " + message.prependIndent().trimStart())

            WARN -> logger.warn("w: [$logTag] " + message.prependIndent().trimStart())

            ERROR -> logger.error("e: [$logTag] " + message.prependIndent().trimStart())
        }
        logWriter.appendLine("[${level.name}] $message")
    }

    override fun close() {
        logWriter.close()
    }
}
