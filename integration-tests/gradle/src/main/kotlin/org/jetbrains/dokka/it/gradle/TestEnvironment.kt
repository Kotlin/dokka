/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

public object TestEnvironment {
    public val isExhaustive: Boolean = checkNotNull(System.getenv("isExhaustive")) {
        "Missing `isExhaustive` environment variable"
    }.toBoolean()
}

/**
 * Will only return values if [TestEnvironment.isExhaustive] is set to true
 */
public inline fun <reified T> ifExhaustive(vararg values: T): Array<out T> {
    return if (TestEnvironment.isExhaustive) values else emptyArray()
}
