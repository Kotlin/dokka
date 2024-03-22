package org.jetbrains.dokka.uitest.markdown

/**
 * # First level header
 *
 * Defines a scope for new coroutines. Every **coroutine builder** (like [launch], [async], etc.)
 * is an extension on [CoroutineScope] and inherits its [coroutineContext][CoroutineScope.coroutineContext]
 * to automatically propagate all its elements and cancellation.
 *
 * The best ways to obtain a standalone instance of the scope are [CoroutineScope()] and [MainScope()] factory functions,
 * taking care to cancel these coroutine scopes when they are no longer needed (see section on custom usage below for
 * explanation and example).
 *
 * Additional context elements can be appended to the scope using the [plus][CoroutineScope.plus] operator.
 *
 * ## Second level header
 *
 * Manual implementation of this interface is not recommended, __implementation__ by delegation should be preferred instead.
 * By convention, the [context of a scope][CoroutineScope.coroutineContext] should contain an instance of a
 * [job][Job] to enforce the discipline of **structured concurrency** with propagation of cancellation.
 *
 * Every coroutine builder (like [launch], [async], and others)
 * and every scoping function (like [coroutineScope] and [withContext]) provides _its own_ scope
 * with its own [Job] instance into the inner __block of code__ it runs.
 * By convention, they all wait for all the coroutines inside their block to complete before completing themselves,
 * thus enforcing the __structured concurrency__. See [Job] documentation for more details.
 *
 * ### Third level header
 *
 * Android has first-party support for coroutine scope in all entities with the lifecycle.
 * See [the corresponding documentation](https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope).
 *
 * #### Fourth level header
 *
 * `CoroutineScope` should be declared as a property on entities with a well-defined lifecycle that are
 * responsible for launching child coroutines. The corresponding instance of `CoroutineScope` shall be created
 * with either `CoroutineScope()` or `MainScope()`:
 *
 * - `CoroutineScope()` uses the [context][CoroutineContext] provided to it as a parameter for its coroutines
 *   and adds a [Job] if one is not provided as part of the context.
 * - `MainScope()` uses [Dispatchers.Main] for its coroutines and has a [SupervisorJob].
 *
 * ##### Fifth level header
 *
 * **The key part of custom usage of `CoroutineScope` is cancelling it at the end of the lifecycle.**
 * The [CoroutineScope.cancel] extension function shall be used when the entity that was launching coroutines
 * is no longer needed. It cancels all the coroutines that might still be running on behalf of it.
 */
class MarkdownHeaders {}
