package org.kotlintestmpp

actual open class CustomException : Exception {
  actual constructor() : super()
  actual constructor(message: String?) : super(message)
  actual constructor(cause: Throwable?) : super(cause)
  actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}
