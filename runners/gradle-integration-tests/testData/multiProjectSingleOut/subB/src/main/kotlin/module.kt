package s2

import s1.Super
import s1.MyClass
import s1.someCoolThing

/**
 * Just an entry-point
 */
fun main(args: Array<String>) {

}

/**
 * Take a glass of hot water
 */
class Cooler {
    val myClass = MyClass()
    val a = myClass.otherworks()
    val coolest = someCoolThing()
}

/**
 * Powerful
 */
class Superful : Super() {
    /**
     * Overriden magic
     */
    override fun bar() = foo(20) * 2
}