package org.jetbrains.dokka.links

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

/**
 * [DRI] stands for DokkaResourceIdentifier
 */
data class DRI(
    val packageName: String? = null,
    val classNames: String? = null,
    val callable: Callable? = null,
    val target: Int? = null,
    val extra: String? = null
) {
    override fun toString(): String =
        "${packageName.orEmpty()}/${classNames.orEmpty()}/${callable?.name.orEmpty()}/${callable?.signature().orEmpty()}/${target?.toString().orEmpty()}/${extra.orEmpty()}"

    companion object {
        fun from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
            val callable = firstIsInstanceOrNull<CallableDescriptor>()
            val params = callable?.let { listOfNotNull(it.extensionReceiverParameter) + it.valueParameters }.orEmpty()
            DRI(
                firstIsInstanceOrNull<PackageFragmentDescriptor>()?.fqName?.asString(),
                filterIsInstance<ClassDescriptor>().toList().takeIf { it.isNotEmpty() }?.asReversed()
                    ?.joinToString(separator = ".") { it.name.asString() },
                callable?.let { Callable.from(it) },
                firstIsInstanceOrNull<ParameterDescriptor>()?.let { params.indexOf(it) },
                null
            )
        }

        val topLevel = DRI()
    }
}

fun DRI.withClass(name: String) = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

val DRI.parent: DRI
    get() = when {
        extra != null -> copy(extra = null)
        target != null -> copy(target = null)
        callable != null -> copy(callable = null)
        classNames != null -> copy(classNames = classNames.substringBeforeLast('.').takeIf { it.isNotBlank() })
        else -> DRI.topLevel
    }

data class Callable(
    val name: String,
    val receiver: TypeReference? = null,
    val params: List<TypeReference>
) {
    fun signature() = "${receiver?.toString().orEmpty()}#${params.joinToString("#")}"

    companion object {
        fun from(descriptor: CallableDescriptor) = with(descriptor) {
            Callable(
                name.asString(),
                extensionReceiverParameter?.let { TypeReference.from(it) },
                valueParameters.mapNotNull { TypeReference.from(it) }
            )
        }
    }
}

data class TypeReference(val classNames: String, val typeBounds: List<TypeReference> = emptyList()) {
    override fun toString() = classNames + if (typeBounds.isNotEmpty()) {
        "[${typeBounds.joinToString(",")}]"
    } else {
        ""
    }

    companion object {
        fun from(d: ReceiverParameterDescriptor): TypeReference? =
            when (val value = d.value) {
                is ExtensionReceiver -> TypeReference(
                    classNames = value.type.constructorName.orEmpty(),
                    typeBounds = value.type.arguments.map { from(it) }
                )
                else -> run {
                    println("Unknown value type for $d")
                    null
                }
            }

        fun from(d: ValueParameterDescriptor): TypeReference? = from(d.type)

        private fun from(tp: TypeParameterDescriptor): TypeReference =
            TypeReference("", tp.upperBounds.map { from(it) })

        private fun from(t: KotlinType): TypeReference =
            when (val d = t.constructor.declarationDescriptor) {
                is TypeParameterDescriptor -> from(d)
                else -> TypeReference(t.constructorName.orEmpty(), t.arguments.map { from(it) })
            }

        private fun from(t: TypeProjection): TypeReference =
            if (t.isStarProjection) {
                starProjection
            } else {
                from(t.type)
            }

        val starProjection = TypeReference("*")
    }
}

private operator fun <T> List<T>.component6(): T = get(5)

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.fqNameSafe?.asString()

