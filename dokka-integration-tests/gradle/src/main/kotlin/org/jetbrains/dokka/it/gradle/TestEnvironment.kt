/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.jetbrains.dokka.it.systemProperty
import org.junit.jupiter.api.Tag

object TestEnvironment {
    val isExhaustive: Boolean by systemProperty(String::toBoolean)

    val isEnabledDebug: Boolean = System.getenv("ENABLE_DEBUG").toBoolean()
}

/**
 * Will only return values if [TestEnvironment.isExhaustive] is set to true
 */
inline fun <reified T> ifExhaustive(vararg values: T): Array<out T> {
    return if (TestEnvironment.isExhaustive) values else emptyArray()
}
