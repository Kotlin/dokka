/**
 * Summary
 *
 * @sample example1
 * @sample example2
 * @sample X.example3
 * @sample X.Y.example4
 */
val property = "test"

fun example1(node: String) = if (true) {
    println(property)
}

fun example2(node: String) {
    if (true) {
        println(property)
    }
}

class X {
    fun example3(node: String) {
        if (true) {
            println(property)
        }
    }

    class Y {
        fun example4(node: String) {
            if (true) {
                println(property)
            }
        }
    }
}
