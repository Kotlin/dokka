package org.kotlintestmpp.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred

/**
 * Common `expect` declaration
 */
expect fun <T> CoroutineScope.asyncWithDealy(delay: Long, block: suspend () -> T): Deferred<T>

/**
 * Common coroutine extension
 */
fun CoroutineDispatcher.name(): String = TODO("Not implemented")
