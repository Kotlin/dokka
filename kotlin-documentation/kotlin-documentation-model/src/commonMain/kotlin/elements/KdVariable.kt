/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: we should have a `KdProperty` with getter/setter and potentially `field`
//  `field` is present only in case `private` declarations are included - TBD
//  TBD what to do with synthetic properties
// getter and setter could have different visibility, so we should have them? they could also have annotations
// we can't really document getter or setter explicitly
// java synthetic property can have field + get/set. Kotlin with EBH also can have different field type?
// what to do with java field+getField+setField based on visibilities
@SerialName("variable")
@Serializable
public data class KdVariable(
    override val id: KdCallableId,
    override val name: String,
    override val returns: KdReturns,
    val variableKind: KdVariableKind,
    // optionals
    val isMutable: Boolean = false, // isVar or isVal
    val constValue: KdConstValue? = null,
    // TODO: getter and setter? do we need them?
    override val isOverride: Boolean = false,
    override val isStatic: Boolean = false,
    override val receiverParameter: KdReceiverParameter? = null,
    override val contextParameters: List<KdContextParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val inheritedFrom: List<KdCallableId> = emptyList(),
    override val source: KdSource = KdSource.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdCallable()