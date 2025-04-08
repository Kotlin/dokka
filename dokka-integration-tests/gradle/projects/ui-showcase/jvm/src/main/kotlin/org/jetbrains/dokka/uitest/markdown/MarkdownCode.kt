package org.jetbrains.dokka.uitest.markdown

/**
 * Contains examples of Markdown code showcasing Kotlin syntax highlighting.
 *
 * Here's a code block with various Kotlin features to test syntax highlighting:
 *
 * ```
 * // Single-line comment token
 * /* Multi-line comment token */
 * /** Documentation comment token */
 * 
 * // Package declaration to test namespace token
 * package com.example.highlighting
 * 
 * // Imports to test namespace tokens
 * import kotlin.random.Random
 * import kotlin.collections.List
 * 
 * // Type alias to test symbol token
 * typealias Handler<T> = (T) -> Unit
 * typealias AsyncOperation = suspend () -> Unit
 * 
 * // Sealed interface to test class-name and keyword tokens
 * sealed interface State {
 *     object Loading : State
 *     data class Success(val data: String) : State
 *     data class Error(val message: String) : State
 * }
 * 
 * // Class with various token types
 * class SyntaxDemo {
 *     // Properties to test property and symbol tokens
 *     private val number: Int = 42 // number token
 *     protected var text: String = "Hello" // string token
 *     internal const val PI = 3.14159 // number token
 *
 *     protected var url: String = "https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/min.html"
 *
 *     var pattern = Regex("""\b\d{3}-\d{3}-\d{4}\b""")
 *
 *     // Companion with property tokens
 *     companion object {
 *         const val DEBUG = true // boolean token
 *         const val CHAR_SAMPLE = 'A' // char token
 *         @JvmField val EMPTY = "" // string token
 *     }
 *     
 *     // Function to test various tokens
 *     fun calculate(x: Double): Double {
 *         val multiplier = 2.5 // number token
 *         val enabled: Boolean = false // boolean token
 *         
 *         // Operators test
 *         val result = when {
 *             x <= 0 -> x * multiplier
 *             x >= 100 -> x / multiplier
 *             else -> x + multiplier
 *         }
 *         
 *         // Built-in types and functions test
 *         val numbers: List<Int> = listOf(1, 2, 3)
 *         val filtered = numbers
 *             .filter { it > 0 } // lambda and operator tokens
 *             .map { it.toString() } // function token
 *             .joinToString(separator = ", ")
 *         
 *         // String template and escape sequence tokens
 *         println("Result: $result\nFiltered: $filtered")
 *         
 *         return result
 *     }
 *     
 *     // Extension function with operator and symbol tokens
 *     infix fun Int.power(exponent: Int): Int {
 *         require(exponent >= 0) { "Exponent must be non-negative" }
 *         return when (exponent) {
 *             0 -> 1
 *             1 -> this
 *             else -> this * (this power (exponent - 1))
 *         }
 *     }
 * }
 * ```
 *
 * Here's inline code with various token types:
 * `val x: Int = 0`, `fun interface EventHandler`, `object : Runnable`, 
 * `class Sample<T : Any>`, `@Deprecated fun old()`, `var name: String?`
 */
class MarkdownCode {}
