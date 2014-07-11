// this file is not included in sources or tests, you can play with it for debug purposes
// Console run configuration will analyse it and provide lots of debug output
package dokka.playground

fun topLevelFunction() {
}

val topLevelConstantValue = "Hello"

val topLevelValue: String
    get() = "Bye bye"

var topLevelVariable: String
    get() = "Modify me!"
    set(value) {
    }

/**
 * This is a class
 */
class Class {
    fun memberFunction() {
    }

    val memberValue = "Member"
}

/**
 * This is a class with constructor and space after doc
 */

class ClassWithConstructor(val name: String)

/**
 * This is data class with constructor and comment after doc
 * $name Person's name
 * $age Person's age
 */
// irrelevant comment
data class Person(/** Doc at parameter */ val name: String, val age: Int) {}

object Object {
    fun objectFunction() {
    }

    val objectValue: String
    /** one line getter doc */
        get() = "Member"

}

class OuterClass {

    class NestedClass {
        fun nestedClassFunction() {
        }
    }

    inner class InnerClass {
        fun innerClassFunction() {
        }
    }

    object NestedObject {
        fun nestedObjectFunction() {
        }
    }
}