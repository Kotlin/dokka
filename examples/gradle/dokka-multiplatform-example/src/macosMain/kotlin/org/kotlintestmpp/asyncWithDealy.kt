package org.kotlintestmpp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

actual fun <T> CoroutineScope.asyncWithDealy(delay: Long, block: suspend () -> T): Deferred<T> {
    TODO("Not yet implemented")
}
