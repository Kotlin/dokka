/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("constructor")
@Serializable
public data class KdConstructor(
    override val id: KdCallableId,
    override val name: String,
    override val returns: KdReturns, // TODO is it fine?
    // optionals
    val isPrimary: Boolean = false,
    val valueParameters: List<KdValueParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val source: KdSource = KdSource.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val documentation: List<KdDocumentationNode>,
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdCallable() {
    override val isOverride: Boolean get() = false
    override val isStatic: Boolean get() = false
    override val typeParameters: List<KdTypeParameter> get() = emptyList()
    override val receiverParameter: KdReceiverParameter? get() = null
    override val contextParameters: List<KdContextParameter> get() = emptyList()
    override val inheritedFrom: List<KdCallableId> get() = emptyList()
}