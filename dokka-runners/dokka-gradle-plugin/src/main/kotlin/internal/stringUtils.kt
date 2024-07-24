/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal


/**
 * Title case the first char of a string.
 *
 * (Custom implementation because [uppercase] is deprecated, and Dokkatoo should try and be as
 * stable as possible.)
 */
internal fun String.uppercaseFirstChar(): String =
    if (isNotEmpty()) Character.toTitleCase(this[0]) + substring(1) else this
