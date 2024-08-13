/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

@DokkaInternalApi
abstract class HasFormatName {
    abstract val formatName: String

    /** Appends [formatName] to the end of the string, camelcase style, if [formatName] is not null */
    protected fun String.appendFormat(): String =
        this + formatName.uppercaseFirstChar()
}
