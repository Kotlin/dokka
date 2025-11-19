/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// java field, kotlin property (including synthetic one), enum_entry, function, constructor

// represents: constructor (no name), function, property, enum_entry, java field
@Serializable
public data class KdCallableId(
    public val packageName: String,
    public val classNames: String?, // if null -> top-level
    // TODO: how to distinguish between: constructor vs function, property vs function
    public val callableName: String?, // if null -> constructor, `classNames` should be not null
    public val isProperty: Boolean // if false -> function - TODO: should we?
)

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
@SerialName("function")
@Serializable
public data class KdFunction(
    override val name: String, // TODO: what is the name for constructors? `<init>` or ``(empty-string) or class-name?
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

// getter and setter could have different visibility, so we should have them? they could also have annotations
// we can't really document getter or setter explicitly
// java synthetic property can have field + get/set. Kotlin with EBH also can have different field type?
// what to do with java field+getField+setField based on visibilities
@SerialName("property")
@Serializable
public data class KdProperty(
    override val name: String,
    override val returns: KdReturns,
    val variableKind: KdVariableKind,
    // optionals
    val isMutable: Boolean = false, // isVar or isVal
    val constValue: KdConstValue? = null,
    // TODO: getter and setter? do we need them?
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
