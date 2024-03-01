package org.kotlintestmpp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kotlintestmpp.common.Foo

/**
 * Function declared in JVM source set
 *
 * also see the [Foo] class
 * @see org.kotlintestmpp.common.Foo
 */
fun jvm() {}

/**
 * Function declared in JVM source set
 *
 * Function with the same name exists in another source set as well.
 */
fun shared() {}

/**
 * Extension declared in JVM source set
 */
fun CoroutineScope.startConnectionPipeline(
    input: String
): Job = launch { TODO() }

/**
 * Extension declared in JVM source set
 */
fun String.myExtension() = println("test2")


