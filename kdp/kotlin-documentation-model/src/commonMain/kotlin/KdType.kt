/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// TODO: how those are represented???
@Serializable
public sealed class KdType {
    // TODO: null if unknown (String!) :)
    public abstract val isNullable: Boolean?


//    public abstract val isDefinitelyNullable: Boolean
//    public abstract val isDefinitelyNotNullable: Boolean
}

@Serializable
public data class KdClassifierType(
    public val classifierId: KdClassifierId,
    public val typeArguments: List<KdTypeArgument> = emptyList(),
    override val isNullable: Boolean? = null // TODO :)
) : KdType()

@Serializable
public data class KdFunctionalType(
    public val returnType: KdType,
    public val receiverType: KdType? = null,
    public val valueParameterTypes: List<KdType> = emptyList(),
    public val contextParameterTypes: List<KdType> = emptyList(),
    public val isSuspend: Boolean = false,
    override val isNullable: Boolean? = null,
) : KdType()

@Serializable
public data class KdTypeArgument(
    val type: KdType? = null, // if null -> star
    val variance: KdVariance? = null
)
