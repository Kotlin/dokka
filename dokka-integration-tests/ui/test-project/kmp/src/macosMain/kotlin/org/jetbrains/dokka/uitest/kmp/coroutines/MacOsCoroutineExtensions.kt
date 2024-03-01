package org.jetbrains.dokka.uitest.kmp.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

/**
 * MacOS actual implementation for `asyncWithDelay`
 */
actual fun <T> CoroutineScope.asyncWithDealy(delay: Long, block: suspend () -> T): Deferred<T> {
    TODO("Not yet implemented")
}
