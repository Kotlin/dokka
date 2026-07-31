/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// information about source location
// url is used for "source links" to GH/GitLab/etc
// fileName can be used for generating "java-like" API - Google
// line/column - ???
@Serializable
public data class KdSource(
    val language: KdSourceLanguage,
    val fileName: String? = null,
    val line: Int = -1,
    val column: Int = -1,
    val url: String? = null
) {
    public companion object {
        // kotlin language, no source information
        public val KOTLIN: KdSource = KdSource(KdSourceLanguage.KOTLIN)

        // java language, no source information
        public val JAVA: KdSource = KdSource(KdSourceLanguage.JAVA)
    }
}

public enum class KdSourceLanguage {
    KOTLIN, JAVA
    // in future, in case we will start support direct interop with other languages
    // C/OBJ_C - cinterop
    // TYPE_SCRIPT - dukat generated
}

// TODO: source-links to GH could be generated based on declaration source + file-system mapping of declarations to path
//  so along the model, we should generation a mapping from `ID` to GH (or other) sources
//  or, we can embed those into the model
//  this should be controlled by a flag
