/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.documentation.*

@OptIn(ExperimentalDokkaApi::class)
internal fun Bound.toKdType(nullability: KdNullability = KdNullability.NOT_NULLABLE): KdType = when (this) {
    is GenericTypeConstructor -> {
        KdClassifierType(
            classifierId = dri.toKdClassifierId(),
            typeArguments = projections.map(Projection::toKdTypeArgument),
            nullability = nullability
        )
    }

    is FunctionalTypeConstructor -> {
        // projections contain in the order:
        // - context parameters
        val contextParameterTypes = projections.subList(0, contextParametersCount).map(Projection::toKdTypeArgument)
        // - receiver
        val receiverType = if (isExtensionFunction) {
            projections[contextParametersCount].toKdTypeArgument()
        } else null
        // - value parameters
        val valueParameterTypes = projections.subList(
            contextParametersCount + (if (isExtensionFunction) 1 else 0),
            projections.size - 1 // excluding last
        ).map(Projection::toKdTypeArgument)
        // - return type
        val returnType = projections.last().toKdTypeArgument()

        KdFunctionalType(
            returnType = returnType,
            receiverType = receiverType,
            valueParameterTypes = valueParameterTypes,
            contextParameterTypes = contextParameterTypes,
            isSuspend = isSuspendable,
            nullability = nullability
        )
    }

    is TypeParameter -> {
        KdTypeParameterType(
            name = name,
            nullability = nullability
        )
    }

    Dynamic -> KdDynamicType

    // TODO: create constants for java types?
    Void -> KdClassifierType(
        classifierId = KdClassifierId("kotlin", "Unit"),
        typeArguments = emptyList(),
        nullability = nullability
    )

    is JavaObject -> KdClassifierType(
        classifierId = KdClassifierId("kotlin", "Any"),
        typeArguments = emptyList(),
        nullability = nullability
    )

    is PrimitiveJavaType -> KdClassifierType(
        // TODO: recheck conversion
        classifierId = KdClassifierId("kotlin", name.replaceFirstChar(Char::uppercase)),
        typeArguments = emptyList(),
        nullability = nullability
    )

    is DefinitelyNonNullable -> inner.toKdType(KdNullability.DEFINITELY_NOT_NULLABLE)
    is Nullable -> inner.toKdType(KdNullability.NULLABLE)

    // TODO: K2 impl is a bit strange here for typeAliased
    is TypeAliased -> typeAlias.toKdType(nullability)
    is UnresolvedBound -> KdUnresolvedType(name, nullability)
}

private fun Projection.toKdTypeArgument(): KdTypeProjection {
    return when (this) {
        is Star -> KdTypeProjection(null, null)
        is Variance<*> -> KdTypeProjection(
            type = inner.toKdType(),
            variance = when (this) {
                is Invariance<*> -> null
                is Contravariance<*> -> KdVariance.IN
                is Covariance<*> -> KdVariance.OUT
            }
        )

        is Bound -> error("should not happen: $this")
    }
}
