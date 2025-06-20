/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

//@Serializable
//public data class KdFileId(
//    public val path: String, // path from source root, e.g. com/example/string/HelloWorld.kt
//) : KdElementId()

//    : KdElement() {
//    override val documentation: KdDocumentation? get() = null // no docs for file for now
//}

// TODO:
//  needed for javadoc generation (top-level classes facades names) and storing file-level annotations?
@Serializable
public data class KdFile(
//    override val id: KdElementId,
    public val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    public val annotations: List<KdAnnotation> = emptyList(),
)