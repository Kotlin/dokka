/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.configuration.ShowStacktrace

/** Only evaluate and log [msg] when [Logger.isWarnEnabled] is `true`. */
internal fun Logger.warn(msg: () -> String) {
    if (isWarnEnabled) warn(msg())
}

/** Only evaluate and log [msg] when [Logger.isInfoEnabled] is `true`. */
internal fun Logger.info(msg: () -> String) {
    if (isInfoEnabled) warn(msg())
}

/** Only evaluate and log [msg] when [Logger.isDebugEnabled] is `true`. */
internal fun Logger.debug(msg: () -> String) {
    if (isDebugEnabled) warn(msg())
}

/**
 * Logs a warning message.
 *
 * If `gradle.startParameter.showStacktrace == ShowStacktrace.ALWAYS`,
 * then logs the stacktrace.
 * Otherwise, adds a hint to rerun with `--stacktrace`.
 */
internal fun Project.logWarningWithStacktraceHint(
    cause: Throwable,
    msg: () -> String,
) {
    if (logger.isWarnEnabled) {
        if (gradle.startParameter.showStacktrace == ShowStacktrace.ALWAYS) {
            project.logger.warn(msg(), cause)
        } else {
            project.logger.warn("${msg()}. Run with `--stacktrace` for more details.")
        }
    }
}
