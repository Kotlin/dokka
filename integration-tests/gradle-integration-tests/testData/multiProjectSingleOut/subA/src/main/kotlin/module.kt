package s1

/**
 * Coolest one
 */
fun someCoolThing(s: String) = s.repeat(2)

/**
 * Just a class
 */
class MyClass {
    /**
     * Ultimate answer to all questions
     */
    fun otherworks(): Int = 42
}

/**
 * Just a SUPER class
 */
open class Super {
    /**
     * Same as [MyClass.otherworks]
     */
    fun foo(i: Int = 21) = i * 2

    /**
     * magic
     */
    open fun bar() = foo()
}