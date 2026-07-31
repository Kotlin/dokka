/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("typealias")
@Serializable
public data class KdTypealias(
    override val id: KdClassLikeId,
    override val name: String,
    val underlyingType: KdType,
    // optionals
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val actuality: KdActuality? = null,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdClassLike() {
    override val source: KdSource get() = KdSource.KOTLIN
    override val modality: KdModality get() = KdModality.FINAL
    override val isExternal: Boolean get() = false
}
