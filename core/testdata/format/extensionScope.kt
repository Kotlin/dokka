/**
 * Test class with Type-parameter
 */
class Foo<T>

/**
 * Some extension on Foo
 */
fun <T> Foo<T>.ext() {}

/**
 * Correct link: [Foo.ext]
 */
fun test() {}