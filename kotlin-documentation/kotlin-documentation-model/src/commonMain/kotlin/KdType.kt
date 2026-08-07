/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: there should be a way to represent both:
//  int (java primitive), Integer (java object), Int (Kotlin type)
//  Overall, it looks like we just need to have `Primitive` type.
//  Integer = java.lang.Integer
//  Int = kotlin.Int
//  int = primitive(int) - enum
//  something similar for arrays? (`int[]` == `IntArray`) != (`Array<Int>` == `Array<Integer>`)
// if we ignore java-api -> then just `kotlin.Int` should be fine?

// TODO: where to add annotations to types?
// TODO: how those are represented???
@Serializable
public sealed class KdType {
    public abstract val nullability: KdTypeNullability
}

@SerialName("classlike")
@Serializable
public data class KdClassLikeType(
    val classLikeId: KdClassLikeId,
    val typeArguments: List<KdTypeProjection> = emptyList(),
    override val nullability: KdTypeNullability = KdTypeNullability.NOT_NULLABLE
) : KdType()

@SerialName("functional")
@Serializable
public data class KdFunctionalType(
    val returnType: KdTypeProjection,
    val receiverType: KdTypeProjection? = null,
    val valueParameterTypes: List<KdTypeProjection> = emptyList(),
    val contextParameterTypes: List<KdTypeProjection> = emptyList(),
    val isSuspend: Boolean = false,
    override val nullability: KdTypeNullability = KdTypeNullability.NOT_NULLABLE
) : KdType()

private fun <T> test(list: List<T>) {} // `List<T>` is KdClassLikeType(List, KdTypeParameterType(T))

@SerialName("typeParameter")
@Serializable
public data class KdTypeParameterType(
    val name: String,
    override val nullability: KdTypeNullability = KdTypeNullability.NOT_NULLABLE
) : KdType()

@SerialName("dynamic")
@Serializable
public data object KdDynamicType : KdType() {
    override val nullability: KdTypeNullability = KdTypeNullability.NOT_NULLABLE
}

// TODO: KaFlexibleType is not really mapped to any Dokka things, but into `TypeAliased`

// TODO: drop it later?
@SerialName("unresolved")
@Serializable
public data class KdUnresolvedType(
    val message: String,
    override val nullability: KdTypeNullability = KdTypeNullability.NOT_NULLABLE
) : KdType()

@Serializable
public data class KdTypeProjection(
    val type: KdType? = null, // if null -> star
    val variance: KdTypeVariance? = null
)

public enum class KdTypeVariance {
    IN, OUT
}

public enum class KdTypeNullability {
    NULLABLE,
    NOT_NULLABLE,
    DEFINITELY_NOT_NULLABLE,
    FLEXIBLE // e.g. platform types
}
