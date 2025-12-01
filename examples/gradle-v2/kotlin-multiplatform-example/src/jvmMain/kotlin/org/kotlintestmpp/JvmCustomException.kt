package org.kotlintestmpp

/**
 * An exception defined in the `jvmMain` source set.
 */
actual open class CustomException : Exception {
    actual constructor() : super()
    actual constructor(message: String?) : super(message)
    actual constructor(cause: Throwable?) : super(cause)
    actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}
