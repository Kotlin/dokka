package example

import greeteer.Greeter

actual open class Clock {
    actual fun getTime(): String = System.currentTimeMillis().toString()
    actual fun getTimesInMillis(): String = System.currentTimeMillis().toString()

    /**
     * Documentation for onlyJVMFunction on...
     * wait for it...
     * ...JVM!
     */
    fun onlyJVMFunction(): Double = 2.5
    /**
     * Custom equals function
     */
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    open fun getDayOfTheWeek(): String {
        TODO("not implemented")
    }
    actual fun getYear(): String {
        TODO("not implemented")
    }
}

fun clockList() = listOf(Clock())

fun main() {
    Greeter().greet().also { println(it) }
}