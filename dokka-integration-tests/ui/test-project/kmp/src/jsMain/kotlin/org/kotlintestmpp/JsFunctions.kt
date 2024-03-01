package org.kotlintestmpp

/**
 * Function declares in JS source set
 */
fun js() {}

/**
 * Function declared in JS source set.
 *
 * Function with the same name exists in another source set as well.
 */
fun shared() {}

/**
 * Extension declared in JS source set
 */
fun String.myExtension() = println("test")
