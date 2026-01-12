package org.kotlintestmpp

/**
 * An exception defined in the `commonMain` source set.
 */
expect open class CustomException : Exception {
    constructor()
    constructor(message: String?)
    constructor(cause: Throwable?)
    constructor(message: String?, cause: Throwable?)
}
