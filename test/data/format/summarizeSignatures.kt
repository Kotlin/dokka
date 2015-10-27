package kotlin

class Array<T>
class IntArray
class CharArray

/**
 * Returns true if foo.
 */
fun IntArray.foo(predicate: (Int) -> Boolean): Boolean = false

/**
 * Returns true if foo.
 */
fun CharArray.foo(predicate: (Char) -> Boolean): Boolean = false

/**
 * Returns true if foo.
 */
fun <T> Array<T>.foo(predicate: (T) -> Boolean): Boolean = false
