package org.kotlintestmpp.web

/**
 * JS implementation of [WebService].
 */
actual class WebService : AutoCloseable {

    actual fun getException(): Throwable? {
        return null
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }
}
