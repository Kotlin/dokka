package org.jetbrains.dokka.gradle.internal

import org.gradle.api.logging.Logger

/** Only evaluate and log [msg] when [Logger.isWarnEnabled] is `true`. */
internal fun Logger.warn(msg: () -> String) {
  if (isWarnEnabled) warn(msg())
}
