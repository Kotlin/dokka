package org.kotlintestmpp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kotlintestmpp.common.Foo

fun main(args : Array<String>) {
    println("Hello, world!")
}

/**
 * also see the [Foo] class
 * @see org.kotlintestmpp.common.Foo
 */
fun jvm(){}
fun shared(){}
fun CoroutineScope.startConnectionPipeline(
    input: String
): Job = launch { TODO () }

/**
 * Actual function for jvm
 */
actual fun getCurrentDate(): String {
    return "test"
}

fun String.myExtension() = println("test2")


