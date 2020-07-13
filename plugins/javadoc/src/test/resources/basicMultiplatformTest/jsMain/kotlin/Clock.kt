package example

import greeteer.Greeter
import kotlin.js.Date

/**
 * Documentation for actual class Clock in JS
 */
actual open class Clock {
    actual fun getTime() = Date.now().toString()
    fun onlyJsFunction(): Int = 42
    actual fun getTimesInMillis(): String = Date.now().toString()

    actual fun getYear(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun main() {
    Greeter().greet().also { println(it) }
}