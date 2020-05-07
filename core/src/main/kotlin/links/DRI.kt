package org.jetbrains.dokka.links

import com.intellij.psi.*
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
    val target: DriTarget = PointingToDeclaration,
    val extra: String? = null
) {
    override fun toString(): String =
        "${packageName.orEmpty()}/${classNames.orEmpty()}/${callable?.name.orEmpty()}/${callable?.signature().orEmpty()}/$target/${extra.orEmpty()}"

    companion object {
        fun from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
            val callable = firstIsInstanceOrNull<CallableDescriptor>()
            DRI(
                firstIsInstanceOrNull<PackageFragmentDescriptor>()?.fqName?.asString(),
                (filterIsInstance<ClassDescriptor>() + filterIsInstance<TypeAliasDescriptor>()).toList()
                    .takeIf { it.isNotEmpty() }
                    ?.asReversed()
                    ?.joinToString(separator = ".") { it.name.asString() },
                callable?.let { Callable.from(it) },
                DriTarget.from(descriptor)
            )
        }

        fun from(psi: PsiElement) = psi.parentsWithSelf.run {
            val callable = firstIsInstanceOrNull<PsiMethod>()
            val classes = filterIsInstance<PsiClass>().toList()
            DRI(
                classes.lastOrNull()?.qualifiedName?.substringBeforeLast('.', ""),
                classes.toList().takeIf { it.isNotEmpty() }?.asReversed()?.mapNotNull { it.name }?.joinToString("."),
                callable?.let { Callable.from(it) },
                DriTarget.from(psi)
            )
        }
        val topLevel = DRI()
    }
}

val DriOfUnit = DRI("kotlin", "Unit")
val DriOfAny = DRI("kotlin", "Any")

fun DRI.withClass(name: String) = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

fun DRI.withTargetToDeclaration() = copy(target = PointingToDeclaration)

val DRI.parent: DRI
    get() = when {
        extra != null -> copy(extra = null)
        target != PointingToDeclaration -> copy(target = PointingToDeclaration)
        callable != null -> copy(callable = null)
        classNames != null -> copy(classNames = classNames.substringBeforeLast(".", "").takeIf { it.isNotBlank() })
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

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.fqNameSafe?.asString()

sealed class DriTarget {
    override fun toString(): String = this.javaClass.simpleName

    companion object {
        fun from(descriptor: DeclarationDescriptor): DriTarget = descriptor.parentsWithSelf.run {
            return when(descriptor){
                is TypeParameterDescriptor -> PointingToGenericParameters(descriptor.index)
                else -> {
                    val callable = firstIsInstanceOrNull<CallableDescriptor>()
                    val params = callable?.let { listOfNotNull(it.extensionReceiverParameter) + it.valueParameters }.orEmpty()
                    val parameterDescriptor = firstIsInstanceOrNull<ParameterDescriptor>()

                    parameterDescriptor?.let { PointingToCallableParameters(params.indexOf(it)) }
                        ?: PointingToDeclaration
                }
            }
        }

        fun from(psi: PsiElement): DriTarget = psi.parentsWithSelf.run {
            return when(psi) {
                is PsiTypeParameter -> PointingToGenericParameters(psi.index)
                else -> firstIsInstanceOrNull<PsiParameter>()?.let {
                    val callable = firstIsInstanceOrNull<PsiMethod>()
                    val params = (callable?.parameterList?.parameters).orEmpty()
                    PointingToCallableParameters(params.indexOf(it))
                } ?: PointingToDeclaration
            }
        }
    }
}

data class PointingToGenericParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToGenericParameters($parameterIndex)"
}

object PointingToDeclaration: DriTarget()

data class PointingToCallableParameters(val parameterIndex: Int): DriTarget(){
    override fun toString(): String = "PointingToCallableParameters($parameterIndex)"
}

fun DriTarget.nextTarget(): DriTarget = when(this){
        is PointingToGenericParameters -> PointingToGenericParameters(this.parameterIndex+1)
        is PointingToCallableParameters -> PointingToCallableParameters(this.parameterIndex+1)
        else -> this
    }

