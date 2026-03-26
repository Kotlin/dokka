/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.kotlin.documentation.*

internal fun DParameter.toKdReceiverParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdReceiverParameter {
    val tagWrappers = tagWrappers(sourceSet) { it is Receiver }
    return KdReceiverParameter(
        type = type.toKdType(),
        documentation = tagWrappers.filterIsInstance<Receiver>().singleOrNullIfEmpty().toKdDocumentation()
    )
}

internal fun DParameter.toKdValueParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdValueParameter {
    val extraModifiers = extraModifiers(sourceSet)

    return KdValueParameter(
        name = requireNotNull(name) { "Parameter $this does not have a name" },
        type = type.toKdType(),
        defaultValue = when (val expr = extra[DefaultValue]?.expression?.get(sourceSet)) {
            is IntegerConstant -> expr.value.toString()
            is FloatConstant -> expr.value.toString() + "f"
            is DoubleConstant -> expr.value.toString()
            is BooleanConstant -> expr.value.toString()
            is StringConstant -> "\"${expr.value}\""
            is ComplexExpression -> expr.value
            else -> null
        }?.let {
            KdParameterDefaultValue.Const(KdConstValue(it))
        },
        isNoinline = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.NoInline),
        isCrossinline = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.CrossInline),
        isVararg = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.VarArg),
        documentation = parameterDocumentation(sourceSet)
    )
}

internal fun DParameter.toKdContextParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdContextParameter {
    return KdContextParameter(
        name = name,
        type = type.toKdType(),
        documentation = parameterDocumentation(sourceSet)
    )
}

internal fun DTypeParameter.toKdTypeParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdTypeParameter {
    val extraModifiers = extraModifiers(sourceSet)

    return KdTypeParameter(
        name = name,
        upperBounds = bounds.map { it.toKdType() },
        variance = when (variantTypeParameter) {
            is Invariance<*> -> null
            is Contravariance<*> -> KdVariance.IN
            is Covariance<*> -> KdVariance.OUT
        },
        isReified = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Reified),
        documentation = parameterDocumentation(sourceSet)
    )
}

// TODO: recheck what is correct here for parameter - it should be enough to check `Description`?!!!
private fun Documentable.parameterDocumentation(sourceSet: DokkaConfiguration.DokkaSourceSet): List<KdDocumentationNode> {
    val tagWrappers = tagWrappers(sourceSet) { it is Description || it is Param }
    return (
            tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty()
                ?: tagWrappers.filterIsInstance<Param>().singleOrNullIfEmpty()?.also { require(it.name == name) }
            ).toKdDocumentation()
}
