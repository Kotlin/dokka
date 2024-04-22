package org.jetbrains.dokka.uitest.markdown

/**
 *
 * Contains examples of Markdown code.
 *
 * Here's a code block:
 *
 * ```
 * class MyUIClass {
 *     val scope = MainScope() // the scope of MyUIClass, uses Dispatchers.Main
 *
 *     fun destroy() { // destroys an instance of MyUIClass
 *         scope.cancel() // cancels all coroutines launched in this scope
 *         // ... do the rest of cleanup here ...
 *     }
 *
 *     /*
 *      * Note: if this instance is destroyed or any of the launched coroutines
 *      * in this method throws an exception, then all nested coroutines are cancelled.
 *      */
 *     fun showSomeData() = scope.launch { // launched in the main thread
 *        // ... here we can use suspending functions or coroutine builders with other dispatchers
 *        draw(data) // draw in the main thread
 *     }
 * }
 * ```
 *
 * Here's inline code: `this` and `that` and `fun foo()` and `class Omg : MyInterface`
 */
class MarkdownCode {}
