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

sealed class TypeReference {
    abstract val isNullable: Boolean

    companion object {
        fun from(d: ReceiverParameterDescriptor): TypeReference? =
            when (d.value) {
                is ExtensionReceiver -> from(d.type)
                else -> run {
                    println("Unknown value type for $d")
                    null
                }
            }

        fun from(d: ValueParameterDescriptor): TypeReference? =
            from(d.type)


        private fun from(t: KotlinType, self: KotlinType? = null): TypeReference =
            if (t == self)
                if (t.isMarkedNullable) NullableSelfType else SelfType
            else when (val d = t.constructor.declarationDescriptor) {
                is TypeParameterDescriptor -> TypeParam(
                    d.upperBounds.map { from(it, self ?: t) },
                    t.isMarkedNullable
                )
                else -> TypeConstructor(
                    t.constructorName.orEmpty(),
                    t.arguments.map { from(it, self) },
                    t.isMarkedNullable
                )
            }


        private fun from(t: TypeProjection, r: KotlinType? = null): TypeReference =
            if (t.isStarProjection) {
                TypeConstructor("kotlin.Any", emptyList(), isNullable = true)
            } else {
                from(t.type, r)
            }
    }
}

data class JavaClassReference(val name: String): TypeReference() {
    override val isNullable = true
    override fun toString(): String = name
}

data class TypeParam(val bounds: List<TypeReference>, override val isNullable: Boolean) : TypeReference()

data class TypeConstructor(
    val fullyQualifiedName: String,
    val params: List<TypeReference>,
    override val isNullable: Boolean
) : TypeReference() {
    override fun toString() = fullyQualifiedName +
            (if (params.isNotEmpty()) "[${params.joinToString(",")}]" else "") +
            if (isNullable) "?" else ""
}

object SelfType : TypeReference() {
    override val isNullable = false
    override fun toString() = "^"
}

object NullableSelfType : TypeReference() {
    override val isNullable = true
    override fun toString() = "^?"
}

private operator fun <T> List<T>.component6(): T = get(5)

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.fqNameSafe?.asString()

