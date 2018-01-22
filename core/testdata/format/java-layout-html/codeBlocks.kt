package p

/**
 * See that `inline code` here
 *
 * Some full code-block
 * ```Kotlin
 *    println(foo()) // Prints 42
 *    println(foo() - 10) // Prints 32
 * ```
 *
 * Some indented code-block
 *     fun ref() = foo()
 *     val a = 2
 * 
 * @sample p.sample
 */
fun foo(): Int {
    return 42
}


fun sample() {
    println(foo()) // Answer unlimate question of all
    println(foo() * 2) // 84!
}