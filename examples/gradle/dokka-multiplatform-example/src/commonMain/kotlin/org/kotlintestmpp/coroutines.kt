package org.kotlintestmpp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

expect fun <T> CoroutineScope.asyncWithDealy(delay: Long, block: suspend () -> T): Deferred<T>
