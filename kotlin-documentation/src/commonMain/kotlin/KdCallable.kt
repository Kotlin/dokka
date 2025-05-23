/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// represents: constructor (no name), function, property, enum_entry
@Serializable
public data class KdCallableId(
    override val packageName: String,
    public val classNames: String?,
    public val callableName: String?, // if null -> constructor
    public val parametersHash: String? // if null -> no parameters?
) : KdDeclarationId() {
    public val classifierId: KdClassifierId? get() = classNames?.let { KdClassifierId(packageName, it) }
}

@Serializable
public sealed class KdCallable : KdDeclaration() {
    abstract override val id: KdCallableId

    public abstract val isStatic: Boolean
    public abstract val isExternal: Boolean // also true if it's in external class

    public abstract val returns: KdReturns
    public abstract val throws: List<KdThrows>
    public abstract val parameters: List<KdParameter> // property/field has no `value` parameters, shoud be sorted
}

// data class
@SerialName("constructor")
@Serializable
public data class KdConstructor(
    override val id: KdCallableId,

    override val returns: KdReturns,

    public val isPrimary: Boolean = false,

    override val throws: List<KdThrows> = emptyList(),
    override val parameters: List<KdParameter> = emptyList(),

    override val isStatic: Boolean = false,
    override val isExternal: Boolean = false,

    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,

    override val documentation: KdDocumentation?,
    override val tags: List<KdTag> = emptyList(),
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdCallable()

// TODO: how to handle overrides?
// data class
@SerialName("function")
@Serializable
public data class KdFunction(
    override val id: KdCallableId,
    override val returns: KdReturns,

    override val throws: List<KdThrows> = emptyList(),
    override val parameters: List<KdParameter> = emptyList(),

    public val isSuspend: Boolean = false,
    public val isOperator: Boolean = false,
    public val isInfix: Boolean = false,
    public val isInline: Boolean = false,
    public val isTailRec: Boolean = false,

    override val isStatic: Boolean = false,
    override val isExternal: Boolean = false,

    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,

    override val documentation: KdDocumentation?,
    override val tags: List<KdTag> = emptyList(),
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdCallable()

@SerialName("variable")
@Serializable
public data class KdVariable(
    override val id: KdCallableId,

    public val variableKind: KdVariableKind,
    override val returns: KdReturns,

    override val throws: List<KdThrows> = emptyList(),
    override val parameters: List<KdParameter> = emptyList(),

    public val isMutable: Boolean = false,
    public val constValue: KdConstValue? = null,

    override val isStatic: Boolean = false,
    override val isExternal: Boolean = false,

    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,

    override val documentation: KdDocumentation?,
    override val tags: List<KdTag> = emptyList(),
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdCallable()
