/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import java.io.File
import java.io.Writer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Logs all Dokka messages to a file.
 *
 * @see org.jetbrains.dokka.DokkaGenerator
 */
// Gradle causes OOM errors when there is a lot of console output. Logging to file is a workaround.
// https://github.com/gradle/gradle/issues/23965
// https://github.com/gradle/gradle/issues/15621
internal class LoggerAdapter(
    outputFile: File
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

    override fun debug(message: String) = log(LoggingLevel.DEBUG, message)
    override fun progress(message: String) = log(LoggingLevel.PROGRESS, message)
    override fun info(message: String) = log(LoggingLevel.INFO, message)

    override fun warn(message: String) {
        warningsCount++
        log(LoggingLevel.WARN, message)
    }

    override fun error(message: String) {
        errorsCount++
        log(LoggingLevel.ERROR, message)
    }

    @Synchronized
    private fun log(level: LoggingLevel, message: String) {
        logWriter.appendLine("[${level.name}] $message")
    }

    override fun close() {
        logWriter.close()
    }
}
