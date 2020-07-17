package org.jetbrains.dokka.analysis

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

fun TypeReference.Companion.from(d: ReceiverParameterDescriptor): TypeReference? =
    when (d.value) {
        is ExtensionReceiver -> fromPossiblyNullable(d.type)
        else -> run {
            println("Unknown value type for $d")
            null
        }
    }

fun TypeReference.Companion.from(d: ValueParameterDescriptor): TypeReference? =
    fromPossiblyNullable(d.type)

fun TypeReference.Companion.from(p: PsiClass) = TypeReference

private fun TypeReference.Companion.fromPossiblyNullable(t: KotlinType, self: KotlinType? = null): TypeReference =
    from(t, self).let { if (t.isMarkedNullable) Nullable(it) else it }

private fun TypeReference.Companion.from(t: KotlinType, self: KotlinType? = null): TypeReference =
    if (self is KotlinType && self.constructor == t.constructor && self.arguments == t.arguments)
        SelfType
    else when (val d = t.constructor.declarationDescriptor) {
        is TypeParameterDescriptor -> TypeParam(
            d.upperBounds.map { fromPossiblyNullable(it, self ?: t) }
        )
        else -> TypeConstructor(
            t.constructorName.orEmpty(),
            t.arguments.map { fromProjection(it, self) }
        )
    }

private fun TypeReference.Companion.fromProjection(t: TypeProjection, r: KotlinType? = null): TypeReference =
    if (t.isStarProjection) {
        StarProjection
    } else {
        fromPossiblyNullable(t.type, r)
    }

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.fqNameSafe?.asString()
