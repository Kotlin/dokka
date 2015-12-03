package kotlin

class Array<T>
class IntArray
class CharArray

/**
 * Returns true if foo.
 */
val IntArray.foo: Int = 0

/**
 * Returns true if foo.
 */
val CharArray.foo: Int = 0

/**
 * Returns true if foo.
 */
val <T> Array<T>.foo: Int = 0
