/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.junit.jupiter.api.Tag

object TestEnvironment {
    const val TRY_K2: String = "org.jetbrains.dokka.experimental.tryK2"

    val isExhaustive: Boolean = checkNotNull(System.getenv("isExhaustive")) {
        "Missing `isExhaustive` environment variable"
    }.toBoolean()

    val isEnabledDebug: Boolean = System.getenv("ENABLE_DEBUG").toBoolean()

    /**
     * By default, it is disabled
     */
    fun shouldUseK2(): Boolean = getBooleanProperty(TRY_K2)

    private fun getBooleanProperty(propertyName: String): Boolean {
        return System.getProperty(propertyName) in setOf("1", "true")
    }
}

/**
 * Will only return values if [TestEnvironment.isExhaustive] is set to true
 */
inline fun <reified T> ifExhaustive(vararg values: T): Array<out T> {
    return if (TestEnvironment.isExhaustive) values else emptyArray()
}

/**
 * Run a test only for descriptors (K1), not symbols (K2).
 * The test with this annotation will be ignored only if the property [TestEnvironment.TRY_K2] = true.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptors")
annotation class OnlyDescriptors(val reason: String = "")
