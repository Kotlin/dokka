/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// both kdoc/javadoc @throws tag and java `throws` keyword
@Serializable
public data class KdThrows(
    public val classifierId: KdClassifierId,
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdDocumented

// return type + @return tag
@Serializable
public data class KdReturns(
    public val type: KdType,
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdDocumented

// generics
@Serializable
public data class KdTypeParameter(
    public val name: String,
    public val upperBounds: List<KdType>,
    public val variance: KdVariance? = null,
    public val isReified: Boolean = false,
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdDocumented

// parameter = receiver, context, value
@Serializable
public sealed class KdParameter : KdDocumented {
    public abstract val name: String?
    public abstract val type: KdType
}

@SerialName("receiver")
@Serializable
public data class KdReceiverParameter(
    override val type: KdType,
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdParameter() {
    override val name: String? get() = null
}

@SerialName("context")
@Serializable
public data class KdContextParameter(
    override val name: String?,
    override val type: KdType,
    override val documentation: List<KdDocumentationNode> = emptyList()
) : KdParameter()

@SerialName("value")
@Serializable
public data class KdValueParameter(
    override val name: String,
    override val type: KdType,
    override val documentation: List<KdDocumentationNode> = emptyList(),
    public val defaultValue: KdParameterDefaultValue? = null,
    public val isNoinline: Boolean = false,
    public val isCrossinline: Boolean = false,
    public val isVararg: Boolean = false
) : KdParameter()

// TODO: take a look on AA `KaCallableSignature`
@Serializable
public sealed class KdParameterDefaultValue {
    @SerialName("const")
    @Serializable
    public data class Const(public val value: KdConstValue) : KdParameterDefaultValue()

    // TODO: it's really hard to support all possible cases here
    //  but it should be rather easy to support simple expressions like `TYPE.FUNCTION(1, x)` or `TYPE.ENUM` or `CLASS::`
    @SerialName("expression")
    @Serializable
    public data class Expression(public val text: String) : KdParameterDefaultValue()

    // complex expression - link to parameter, function call, etc
}
