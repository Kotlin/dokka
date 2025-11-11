/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// java field, kotlin property (including synthetic one), enum_entry, function, constructor

@Serializable
public sealed class KdCallable : KdDeclaration() {
    public abstract val returns: KdReturns

    public abstract val isStatic: Boolean // do nothing until static KEEP?
    public abstract val receiverParameter: KdReceiverParameter?
    public abstract val valueParameters: List<KdValueParameter>
    public abstract val contextParameters: List<KdContextParameter>
    public abstract val throws: List<KdThrows>
}

@SerialName("constructor")
@Serializable
public data class KdConstructor(
    override val name: String,
    override val returns: KdReturns, // TODO is it fine?
    // optionals
    val isPrimary: Boolean = false,
    override val valueParameters: List<KdValueParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val documentation: KdDocumentation?,
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdCallable() {
    override val isStatic: Boolean get() = false
    override val typeParameters: List<KdTypeParameter> get() = emptyList()
    override val receiverParameter: KdReceiverParameter? get() = null
    override val contextParameters: List<KdContextParameter> get() = emptyList()
}

// TODO: how to handle overrides?
// data class
@SerialName("function")
@Serializable
public data class KdFunction(
    override val name: String,
    override val returns: KdReturns,
    // optionals
    val isSuspend: Boolean = false,
    val isOperator: Boolean = false,
    val isInfix: Boolean = false,
    val isInline: Boolean = false,
    val isTailRec: Boolean = false,
    override val isStatic: Boolean = false,
    override val receiverParameter: KdReceiverParameter? = null,
    override val valueParameters: List<KdValueParameter> = emptyList(),
    override val contextParameters: List<KdContextParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: KdDocumentation? = null,
) : KdCallable()

// TODO: probably we need to have `getter/setter` here to be able to correctly generate javadoc from this
@SerialName("variable")
@Serializable
public data class KdVariable(
    override val name: String,
    override val returns: KdReturns,
    val variableKind: KdVariableKind,
    // optionals
    val isMutable: Boolean = false,
    val constValue: KdConstValue? = null,
    override val isStatic: Boolean = false,
    override val receiverParameter: KdReceiverParameter? = null,
    override val contextParameters: List<KdContextParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: KdDocumentation? = null,
) : KdCallable() {
    override val valueParameters: List<KdValueParameter> get() = emptyList()
}
