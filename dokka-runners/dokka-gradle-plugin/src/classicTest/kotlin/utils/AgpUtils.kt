/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.utils

/**
 * AGP 7+ is compiled with Java 8, but requires Java 11+ to run:
 *
 * > EvalIssueException: Android Gradle plugin requires Java 11 to run. You are currently using Java 1.8.
 */
fun isAgpRunnable(): Boolean {
    val javaVersion = when (val specVersion = System.getProperty("java.specification.version")) {
        "1.8" -> 8
        else -> specVersion.toInt()
    }
    return javaVersion >= 11
}
