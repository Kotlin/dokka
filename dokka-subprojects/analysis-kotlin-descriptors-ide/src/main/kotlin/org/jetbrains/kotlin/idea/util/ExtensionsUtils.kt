/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("ExtensionUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.nullability


internal fun <TCallable : CallableDescriptor> TCallable.substituteExtensionIfCallable(
    receiverTypes: Collection<KotlinType>,
    callType: CallType<*>,
    ignoreTypeParameters: Boolean = false,
): Collection<TCallable> {
    if (!callType.descriptorKindFilter.accepts(this)) return listOf()

    var types = receiverTypes.asSequence()
    if (callType == CallType.SAFE) {
        types = types.map { it.makeNotNullable() }
    }

    val extensionReceiverType = fuzzyExtensionReceiverType()!!
    val substitutors = types.mapNotNull {
        // NOTE: this creates a fuzzy type for `it` without type parameters
        var substitutor = extensionReceiverType.checkIsSuperTypeOf(it)

        // If enabled, we can ignore type parameters in the receiver type, and only check whether the constructors match
        if (ignoreTypeParameters && substitutor == null && it.constructor == extensionReceiverType.type.constructor) {
            substitutor = TypeSubstitutor.EMPTY
        }

        // check if we may fail due to receiver expression being nullable
        if (substitutor == null && it.nullability() == TypeNullability.NULLABLE && extensionReceiverType.nullability() == TypeNullability.NOT_NULL) {
            substitutor = extensionReceiverType.checkIsSuperTypeOf(it.makeNotNullable())
        }
        substitutor
    }

    return if (typeParameters.isEmpty()) { // optimization for non-generic callables
        if (substitutors.any()) listOf(this) else listOf()
    } else {
        substitutors
            .mapNotNull { @Suppress("UNCHECKED_CAST") (substitute(it) as TCallable?) }
            .toList()
    }
}
