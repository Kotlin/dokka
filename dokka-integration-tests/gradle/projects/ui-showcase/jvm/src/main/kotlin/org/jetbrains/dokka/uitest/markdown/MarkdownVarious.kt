package org.jetbrains.dokka.uitest.markdown

/**
 * Various markdown elements.
 *
 * This class implements completion [Continuation], [Job], and [CoroutineScope] interfaces.
 * It stores **the result** of continuation in the state of the job.
 * This coroutine waits for _children_ coroutines to finish before completing and
 * fails through an intermediate _failing_ state.
 *
 * - [onStart] is invoked when the coroutine was created in non-active state and is being [started][Job.start].
 * - [onCancelling] is invoked as soon as the coroutine starts being cancelled for any reason (or completes).
 * - [onCompleted] is invoked when the coroutine completes with a value.
 * - [onCancelled] in invoked when the coroutine completes with an exception (cancelled).
 */
class MarkdownVarious {
}
