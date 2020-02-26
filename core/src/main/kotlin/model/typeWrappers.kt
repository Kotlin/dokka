package org.jetbrains.dokka.model

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

interface TypeWrapper {
    val constructorFqName: String?
    val constructorNamePathSegments: List<String>
    val arguments: List<TypeWrapper>
    val dri: DRI?
}

class KotlinTypeWrapper(private val kotlinType: KotlinType) : TypeWrapper {
    private val declarationDescriptor = kotlinType.constructor.declarationDescriptor
    private val fqNameSafe = declarationDescriptor?.fqNameSafe
    override val constructorFqName = fqNameSafe?.asString()
    override val constructorNamePathSegments: List<String> =
        fqNameSafe?.pathSegments()?.map { it.asString() } ?: emptyList()
    override val arguments: List<KotlinTypeWrapper> by lazy {
        kotlinType.arguments.map {
            KotlinTypeWrapper(
                it.type
            )
        }
    }
    override val dri: DRI? by lazy { declarationDescriptor?.let { DRI.from(it) } }
}

class JavaTypeWrapper : TypeWrapper {

    override val constructorFqName: String?
    override val constructorNamePathSegments: List<String>
    override val arguments: List<TypeWrapper>
    override val dri: DRI?
    val isPrimitive: Boolean

    constructor(
        constructorNamePathSegments: List<String>,
        arguments: List<TypeWrapper>,
        dri: DRI?,
        isPrimitiveType: Boolean
    ) {
        this.constructorFqName = constructorNamePathSegments.joinToString(".")
        this.constructorNamePathSegments = constructorNamePathSegments
        this.arguments = arguments
        this.dri = dri
        this.isPrimitive = isPrimitiveType
    }

    constructor(type: PsiType) {
        if (type is PsiClassReferenceType) {
            val resolved = type.resolve()
            constructorFqName = resolved?.qualifiedName
            constructorNamePathSegments = resolved?.qualifiedName?.split('.') ?: emptyList()
            arguments = type.parameters.mapNotNull {
                if (it is PsiClassReferenceType) JavaTypeWrapper(it) else null
            }
            dri = fromPsi(type)
            this.isPrimitive = false
        } else if (type is PsiEllipsisType) {
            constructorFqName = type.canonicalText
            constructorNamePathSegments = listOf(type.canonicalText) // TODO
            arguments = emptyList()
            dri = DRI("java.lang", "Object") // TODO
            this.isPrimitive = false
        } else if (type is PsiArrayType) {
            constructorFqName = type.canonicalText
            constructorNamePathSegments = listOf(type.canonicalText)
            arguments = emptyList()
            dri = (type as? PsiClassReferenceType)?.let { fromPsi(it) } // TODO
            this.isPrimitive = false
        } else {
            type as PsiPrimitiveType
            constructorFqName = type.name
            constructorNamePathSegments = type.name.split('.')
            arguments = emptyList()
            dri = null
            this.isPrimitive = true
        }
    }

    private fun fromPsi(type: PsiClassReferenceType): DRI {
        val className = type.className
        val pkg = type.canonicalText.removeSuffix(className).removeSuffix(".")
        return DRI(packageName = pkg, classNames = className)
    }

    override fun toString(): String {
        return constructorFqName.orEmpty()
    }

    companion object {
        val VOID = JavaTypeWrapper(listOf("void"), listOf(), null, true)
    }
}
