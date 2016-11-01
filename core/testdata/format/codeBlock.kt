import kotlin.reflect.KClass

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a JVM method.
 *
 * Example:
 *
 * ```
 * Throws(IOException::class)
 * fun readFile(name: String): String {...}
 * ```
 */
class Throws


/**
 * Check output of
 * ``` brainfuck
 * ++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>.
 * ```
 */
class ItDoesSomeObfuscatedThing