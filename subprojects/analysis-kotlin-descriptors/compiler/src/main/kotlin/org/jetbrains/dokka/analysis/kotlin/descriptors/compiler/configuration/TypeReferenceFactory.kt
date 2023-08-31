/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeConstructor
import org.jetbrains.kotlin.types.error.ErrorTypeKind

internal fun TypeReference.Companion.from(d: ReceiverParameterDescriptor): TypeReference? =
    when (d.value) {
        is ExtensionReceiver -> fromPossiblyNullable(d.type, emptyList())
        else -> run {
            println("Unknown value type for $d")
            null
        }
    }

internal fun TypeReference.Companion.from(d: ValueParameterDescriptor): TypeReference =
    fromPossiblyNullable(d.type, emptyList())

internal fun TypeReference.Companion.from(@Suppress("UNUSED_PARAMETER") p: PsiClass) = TypeReference

private fun TypeReference.Companion.fromPossiblyNullable(t: KotlinType, paramTrace: List<KotlinType>): TypeReference =
    fromPossiblyRecursive(t, paramTrace).let { if (t.isMarkedNullable) Nullable(it) else it }

private fun TypeReference.Companion.fromPossiblyRecursive(t: KotlinType, paramTrace: List<KotlinType>): TypeReference =
    paramTrace.indexOfFirst { it.constructor == t.constructor && it.arguments == t.arguments }
        .takeIf { it >= 0 }
        ?.let(::RecursiveType)
        ?: from(t, paramTrace)

private fun TypeReference.Companion.from(t: KotlinType, paramTrace: List<KotlinType>): TypeReference {
    if (t is ErrorType) {
        val errorConstructor = t.constructor as? ErrorTypeConstructor
        val presentableName =
            if (errorConstructor?.kind == ErrorTypeKind.UNRESOLVED_TYPE && errorConstructor.parameters.isNotEmpty())
                errorConstructor.getParam(0)
            else
                t.constructor.toString()
        return TypeConstructor(presentableName, t.arguments.map { fromProjection(it, paramTrace) })
    }
    return when (val d = t.constructor.declarationDescriptor) {
        is TypeParameterDescriptor -> TypeParam(
            d.upperBounds.map { fromPossiblyNullable(it, paramTrace + t) }
        )
        else -> TypeConstructor(
            t.constructorName.orEmpty(),
            t.arguments.map { fromProjection(it, paramTrace) }
        )
    }
}

private fun TypeReference.Companion.fromProjection(t: TypeProjection, paramTrace: List<KotlinType>): TypeReference =
    if (t.isStarProjection) {
        StarProjection
    } else {
        fromPossiblyNullable(t.type, paramTrace)
    }

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.fqNameSafe?.asString()
