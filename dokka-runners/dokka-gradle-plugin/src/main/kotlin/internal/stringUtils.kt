/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal


/** [Title case][Character.toTitleCase] the first char of a string. */
// Remove when min supported Gradle >= 8.0
internal fun String.uppercaseFirstChar(): String =
    if (isNotEmpty()) Character.toTitleCase(this[0]) + substring(1) else this

// Workaround for being forced to use Kotlin 1.4 by Gradle, but cursed by deprecation warnings from IntelliJ.
// Remove when min supported Gradle >= 8.0
internal fun String.lowercase(): String = toLowerCase()
