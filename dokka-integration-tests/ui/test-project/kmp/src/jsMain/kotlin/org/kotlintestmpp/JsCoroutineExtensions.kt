package org.kotlintestmpp.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

/**
 * JS actual implementation for `asyncWithDelay`
 */
actual fun <T> CoroutineScope.asyncWithDealy(delay: Long, block: suspend () -> T): Deferred<T> {
    TODO("Not yet implemented")
}
