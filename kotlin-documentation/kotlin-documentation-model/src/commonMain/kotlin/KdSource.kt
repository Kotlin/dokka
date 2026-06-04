/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public data class KdSource(
    val language: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    val fileName: String? = null,
    // TODO: or just `offset`
    val line: Int = -1,
    val column: Int = -1
) {
    public companion object {
        // kotlin language, no source information
        public val Kotlin: KdSource = KdSource()
    }
}

public enum class KdSourceLanguage {
    KOTLIN, JAVA
    // C/OBJ_C - cinterop
    // TYPE_SCRIPT - dukat generated
}
