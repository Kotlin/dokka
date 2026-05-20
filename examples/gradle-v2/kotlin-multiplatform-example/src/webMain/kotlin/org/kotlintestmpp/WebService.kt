/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.kotlintestmpp.web

/**
 * Specification of a `WebService`, defined in `webMain`.
 */
expect class WebService : AutoCloseable {
    constructor()

    fun getException(): Throwable?

    override fun close()
}
