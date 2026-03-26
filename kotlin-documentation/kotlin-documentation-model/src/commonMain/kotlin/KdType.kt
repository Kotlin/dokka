/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: where to add annotations to types?
// TODO: how those are represented???
@Serializable
public sealed class KdType {
    public abstract val nullability: KdNullability
}

@SerialName("classifier")
@Serializable
public data class KdClassifierType(
    val classifierId: KdClassifierId,
    val typeArguments: List<KdTypeProjection> = emptyList(),
    override val nullability: KdNullability = KdNullability.NOT_NULLABLE
) : KdType()

@SerialName("functional")
@Serializable
public data class KdFunctionalType(
    val returnType: KdTypeProjection,
    val receiverType: KdTypeProjection? = null,
    val valueParameterTypes: List<KdTypeProjection> = emptyList(),
    val contextParameterTypes: List<KdTypeProjection> = emptyList(),
    val isSuspend: Boolean = false,
    override val nullability: KdNullability = KdNullability.NOT_NULLABLE
) : KdType()

@SerialName("typeParameter")
@Serializable
public data class KdTypeParameterType(
    val name: String,
    override val nullability: KdNullability = KdNullability.NOT_NULLABLE
) : KdType()

@SerialName("dynamic")
@Serializable
public data object KdDynamicType : KdType() {
    override val nullability: KdNullability = KdNullability.NOT_NULLABLE
}

// TODO: KaFlexibleType is not really mapped to any Dokka things, but into `TypeAliased`

// TODO: drop it later?
@SerialName("unresolved")
@Serializable
public data class KdUnresolvedType(
    val message: String,
    override val nullability: KdNullability = KdNullability.NOT_NULLABLE
) : KdType()

@Serializable
public data class KdTypeProjection(
    val type: KdType? = null, // if null -> star
    val variance: KdVariance? = null
)
