package org.jetbrains.dokka.links

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
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
    val genericTarget: Int? = null,
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
                (filterIsInstance<ClassDescriptor>() + filterIsInstance<TypeAliasDescriptor>()).toList()
                    .takeIf { it.isNotEmpty() }
                    ?.asReversed()
                    ?.joinToString(separator = ".") { it.name.asString() },
                callable?.let { Callable.from(it) },
                firstIsInstanceOrNull<ParameterDescriptor>()?.let { params.indexOf(it) },
                null
            )
        }

        fun from(psi: PsiElement) = psi.parentsWithSelf.run {
            val callable = firstIsInstanceOrNull<PsiMethod>()
            val params = (callable?.parameterList?.parameters).orEmpty()
            val classes = filterIsInstance<PsiClass>().toList()
            DRI(
                classes.lastOrNull()?.qualifiedName?.substringBeforeLast('.', ""),
                classes.toList().takeIf { it.isNotEmpty() }?.asReversed()?.mapNotNull { it.name }?.joinToString("."),
                callable?.let { Callable.from(it) },
                firstIsInstanceOrNull<PsiParameter>()?.let { params.indexOf(it) }
            )
        }
        val topLevel = DRI()
    }
}

val DriOfUnit = DRI("kotlin", "Unit")
val DriOfAny = DRI("kotlin", "Any")

fun DRI.withClass(name: String) = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

val DRI.parent: DRI
    get() = when {
        extra != null -> copy(extra = null)
        genericTarget != null -> copy(genericTarget = null)
        target != null -> copy(target = null)
        callable != null -> copy(callable = null)
        classNames != null -> copy(classNames = classNames.substringBeforeLast('.').takeIf { it.isNotBlank() })
        else -> DRI.topLevel
    }

val DRI.sureClassNames
    get() = classNames ?: throw IllegalStateException("Malformed DRI. It requires classNames in this context.")

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
        fun from(psi: PsiMethod) = with(psi) {
            Callable(
                name,
                null,
                parameterList.parameters.map { param -> JavaClassReference(param.type.canonicalText) })
        }
    }
}

sealed class TypeReference {
    companion object {
        fun from(d: ReceiverParameterDescriptor): TypeReference? =
            when (d.value) {
                is ExtensionReceiver -> fromPossiblyNullable(d.type)
                else -> run {
                    println("Unknown value type for $d")
                    null
                }
            }

        fun from(d: ValueParameterDescriptor): TypeReference? =
            fromPossiblyNullable(d.type)

        fun from(p: PsiClass) = TypeReference

        private fun fromPossiblyNullable(t: KotlinType, self: KotlinType? = null): TypeReference =
            from(t, self).let { if (t.isMarkedNullable) Nullable(it) else it }

        private fun from(t: KotlinType, self: KotlinType? = null): TypeReference =
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

        private fun fromProjection(t: TypeProjection, r: KotlinType? = null): TypeReference =
            if (t.isStarProjection) {
                StarProjection
            } else {
                fromPossiblyNullable(t.type, r)
            }
    }
}

data class JavaClassReference(val name: String) : TypeReference() {
    override fun toString(): String = name
}

data class TypeParam(val bounds: List<TypeReference>) : TypeReference()

data class TypeConstructor(
    val fullyQualifiedName: String,
    val params: List<TypeReference>
) : TypeReference() {
    override fun toString() = fullyQualifiedName +
            (if (params.isNotEmpty()) "[${params.joinToString(",")}]" else "")
}

object SelfType : TypeReference() {
    override fun toString() = "^"
}

data class Nullable(val wrapped: TypeReference) : TypeReference() {
    override fun toString() = "$wrapped?"
}

object StarProjection : TypeReference() {
    override fun toString() = "*"
}

private operator fun <T> List<T>.component6(): T = get(5)

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.fqNameSafe?.asString()

