/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public data class KdExternalReferences(
    val references: Map<KdSymbolId, KdSymbolExternalReferenceInfo>
)

public data class KdSymbolExternalReferenceInfo(
    // displayName
    val name: String,
    val url: String,
    // some metadata?: language?(java/kotlin), type?(function/class/property)
)

// both external references and symbols should be defined per fragment

public data class KdSourceReferences(
    val references: Map<KdSymbolId, KdSymbolExternalReferenceInfo>
)

public data class KdSymbolSourceReferenceInfo(
    val name: String,
    val url: String,
    // range for the link (startAt, endsAt), etc.
)
