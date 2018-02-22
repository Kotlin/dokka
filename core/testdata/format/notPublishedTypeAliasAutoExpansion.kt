
class A
class B


internal typealias TA = A
private typealias TB = B

/**
 * Correct ref [TA]
 * Correct ref [TB]
 */
fun foo() {}