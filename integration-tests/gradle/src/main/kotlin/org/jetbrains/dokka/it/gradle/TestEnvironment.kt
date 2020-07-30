package org.jetbrains.dokka.it.gradle

object TestEnvironment {
    val isExhaustive = checkNotNull(System.getenv("isExhaustive")) {
        "Missing `isExhaustive` environment variable"
    }.toBoolean()
}

/**
 * Will only return values if [TestEnvironment.isExhaustive] is set to true
 */
inline fun <reified T> ifExhaustive(vararg values: T): Array<out T> {
    return if (TestEnvironment.isExhaustive) values else emptyArray()
}
