/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.kotlintestmpp.web

/**
 * WasmJS implementation of [WebService].
 */
actual open class WebService : AutoCloseable {
  actual constructor() : super()

  actual fun getException(): Throwable? {
    return null
  }

  actual override fun close() {
    TODO("Not yet implemented")
  }
}
