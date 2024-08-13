/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.logging.Logger

/** Only evaluate and log [msg] when [Logger.isWarnEnabled] is `true`. */
internal fun Logger.warn(msg: () -> String) {
    if (isWarnEnabled) warn(msg())
}
