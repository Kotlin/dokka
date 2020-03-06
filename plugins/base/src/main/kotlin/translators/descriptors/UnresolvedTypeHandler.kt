package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.OtherParameter
import org.jetbrains.dokka.plugability.UnresolvedTypePolicy
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

class UnresolvedTypeHandler(val typePolicy: UnresolvedTypePolicy) {

    fun convertKotlinType(type: KotlinType?, defaultTypeProcessor: (KotlinType) -> Bound): Bound? = type.let { type ->
        if (type?.isError == false) {
            defaultTypeProcessor(type!!)
        } else {
            when (typePolicy) {
                UnresolvedTypePolicy.Exception -> throw IllegalStateException("Unresolved type: $type")
                UnresolvedTypePolicy.Skip -> null
                UnresolvedTypePolicy.Approximate -> when (val t = type) {
                    is org.jetbrains.kotlin.types.UnresolvedType -> t.presentableName
                    is org.jetbrains.kotlin.types.DeferredType -> "Error type"
                    else ->
                        org.jetbrains.dokka.utilities.DokkaConsoleLogger.info(t.toString()).let { null }
                }
            }?.let { OtherParameter(it) }
        }
    }


    companion object {
        inline fun <reified T : DeclarationDescriptor> Collection<T>.filterTypeError(typePolicy: UnresolvedTypePolicy) =
            if (typePolicy == UnresolvedTypePolicy.Skip) {
                this.filterNot { it.isErrorType() }
            } else this

        fun DeclarationDescriptor.isErrorType() =
            (this is FunctionDescriptor && this.returnType?.isError != false) ||
                    (this is ValueParameterDescriptor && this.type.isError) ||
                    (this is PropertyDescriptor && this.type.isError) ||
                    (this is ReceiverParameterDescriptor && this.type.isError)

        fun DeclarationDescriptor.getType() = when (this) {
            is ValueParameterDescriptor -> type
            is FunctionDescriptor -> returnType
            is PropertyDescriptor -> returnType
            is ReceiverParameterDescriptor -> type
            else -> null
        }
    }
}