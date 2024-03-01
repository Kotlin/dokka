package org.jetbrains.dokka.uitest.markdown

/**
 * This class has markdown tables.
 *
 * Core primitives to work with coroutines.
 *
 * Coroutine builder functions:
 *
 * | **Name**                                       | **Result**                                                   | **Scope**                                                  | **Description**
 * | ---------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------- | ---------------
 * | [launch][kotlinx.coroutines.launch]            | [Job][kotlinx.coroutines.Job]                                | [CoroutineScope][kotlinx.coroutines.CoroutineScope]        | Launches coroutine that does not have any result
 * | [async][kotlinx.coroutines.async]              | [Deferred][kotlinx.coroutines.Deferred]                      | [CoroutineScope][kotlinx.coroutines.CoroutineScope]        | Returns a single value with the future result
 * | [produce][kotlinx.coroutines.channels.produce] | [ReceiveChannel][kotlinx.coroutines.channels.ReceiveChannel] | [ProducerScope][kotlinx.coroutines.channels.ProducerScope] | Produces a stream of elements
 * | [runBlocking][kotlinx.coroutines.runBlocking]  | `T`                                                          | [CoroutineScope][kotlinx.coroutines.CoroutineScope]        | Blocks the thread while the coroutine runs
 *
 * Coroutine dispatchers implementing [CoroutineDispatcher]:
 *
 * | **Name**                                                            | **Description**
 * | ------------------------------------------------------------------- | ---------------
 * | [Dispatchers.Default][kotlinx.coroutines.Dispatchers.Default]       | Confines coroutine execution to a shared pool of background threads
 * | [Dispatchers.Unconfined][kotlinx.coroutines.Dispatchers.Unconfined] | Does not confine coroutine execution in any way
 *
 * More context elements:
 *
 * | **Name**                                                                  | **Description**
 * | ------------------------------------------------------------------------- | ---------------
 * | [NonCancellable][kotlinx.coroutines.NonCancellable]                       | A non-cancelable job that is always active
 * | [CoroutineExceptionHandler][kotlinx.coroutines.CoroutineExceptionHandler] | Handler for uncaught exception
 *
 * Synchronization primitives for coroutines:
 *
 * | **Name**                                        | **Suspending functions**                                                                                            | **Description**
 * | ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- | ---------------
 * | [Mutex][kotlinx.coroutines.sync.Mutex]          | [lock][kotlinx.coroutines.sync.Mutex.lock]                                                                          | Mutual exclusion
 * | [Channel][kotlinx.coroutines.channels.Channel]  | [send][kotlinx.coroutines.channels.SendChannel.send], [receive][kotlinx.coroutines.channels.ReceiveChannel.receive] | Communication channel (aka queue or exchanger)
 *
 * Top-level suspending functions:
 *
 * | **Name**                                                  | **Description**
 * | --------------------------------------------------------- | ---------------
 * | [delay][kotlinx.coroutines.delay]                         | Non-blocking sleep
 * | [yield][kotlinx.coroutines.yield]                         | Yields thread in single-threaded dispatchers
 * | [withContext][kotlinx.coroutines.withContext]             | Switches to a different context
 * | [withTimeout][kotlinx.coroutines.withTimeout]             | Set execution time-limit with exception on timeout
 * | [withTimeoutOrNull][kotlinx.coroutines.withTimeoutOrNull] | Set execution time-limit will null result on timeout
 * | [awaitAll][kotlinx.coroutines.awaitAll]                   | Awaits for successful completion of all given jobs or exceptional completion of any
 * | [joinAll][kotlinx.coroutines.joinAll]                     | Joins on all given jobs
 *
 * Cancellation support for user-defined suspending functions is available with [suspendCancellableCoroutine]
 * helper function. [NonCancellable] job object is provided to suppress cancellation with
 * `withContext(NonCancellable) {...}` block of code.
 *
 * [Select][kotlinx.coroutines.selects.select] expression waits for the result of multiple suspending functions simultaneously:
 *
 * | **Receiver**                                                 | **Suspending function**                                         | **Select clause**                                                 | **Non-suspending version**
 * | ------------------------------------------------------------ | --------------------------------------------------------------- | ----------------------------------------------------------------- | --------------------------
 * | [Job][kotlinx.coroutines.Job]                                | [join][kotlinx.coroutines.Job.join]                             | [onJoin][kotlinx.coroutines.Job.onJoin]                           | [isCompleted][kotlinx.coroutines.Job.isCompleted]
 * | [Deferred][kotlinx.coroutines.Deferred]                      | [await][kotlinx.coroutines.Deferred.await]                      | [onAwait][kotlinx.coroutines.Deferred.onAwait]                    | [isCompleted][kotlinx.coroutines.Job.isCompleted]
 * | [SendChannel][kotlinx.coroutines.channels.SendChannel]       | [send][kotlinx.coroutines.channels.SendChannel.send]            | [onSend][kotlinx.coroutines.channels.SendChannel.onSend]          | [trySend][kotlinx.coroutines.channels.SendChannel.trySend]
 * | [ReceiveChannel][kotlinx.coroutines.channels.ReceiveChannel] | [receive][kotlinx.coroutines.channels.ReceiveChannel.receive]   | [onReceive][kotlinx.coroutines.channels.ReceiveChannel.onReceive] | [tryReceive][kotlinx.coroutines.channels.ReceiveChannel.tryReceive]
 * | [ReceiveChannel][kotlinx.coroutines.channels.ReceiveChannel] | [receiveCatching][kotlinx.coroutines.channels.receiveCatching]  | [onReceiveCatching][kotlinx.coroutines.channels.onReceiveCatching] | [tryReceive][kotlinx.coroutines.channels.ReceiveChannel.tryReceive]
 * | none                                                         | [delay][kotlinx.coroutines.delay]                               | [onTimeout][kotlinx.coroutines.selects.SelectBuilder.onTimeout]   | none
 */
class MarkdownTable {}

