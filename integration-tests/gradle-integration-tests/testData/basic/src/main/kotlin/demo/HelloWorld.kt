package demo

import p1.MyBinaryClass

/**
 * This class supports greeting people by name.
 *
 * @property name The name of the person to be greeted.
 */
class Greeter(val name: String) {

    /**
     * Prints the greeting to the standard output.
     */
    fun greet() {
        println("Hello $name!")
    }
}

fun main(args: Array<String>) {
    Greeter(args[0]).greet()
}

val str = "Hello! ".repeat(4)
val x: (a: String, b: Int) -> Int = { a, b -> 0 }

interface SomeInterface
private class SomeImpl : SomeInterface

fun SomeInterface.constructor(): SomeInterface {
    return SomeImpl()
}

open class SomeType
class SomeSubType : SomeType()

fun SomeType.constructor(): SomeType {
    return SomeSubType()
}


annotation class A(val p: String)

val MyBinaryClass.test get() = s()

