/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withEnumEntryExtra
import org.jetbrains.dokka.model.*

/**
 * Computes the constant value of a Java field and converts it to a Dokka [Expression].
 */
public fun PsiField.getConstantExpression(): Expression? {
    val constantValue = this.computeConstantValue() ?: return null
    return when (constantValue) {
        is Byte -> IntegerConstant(constantValue.toLong())
        is Short -> IntegerConstant(constantValue.toLong())
        is Int -> IntegerConstant(constantValue.toLong())
        is Long -> IntegerConstant(constantValue)
        is Char -> StringConstant(constantValue.toString())
        is String -> StringConstant(constantValue)
        is Double -> DoubleConstant(constantValue)
        is Float -> FloatConstant(constantValue)
        is Boolean -> BooleanConstant(constantValue)
        else -> ComplexExpression(constantValue.toString())
    }
}

/**
 * Converts a Java constant value (from [PsiField.computeConstantValue] or annotation attributes)
 * to a Dokka [AnnotationParameterValue].
 */
public fun Any.toAnnotationLiteralValue(): AnnotationParameterValue = when (this) {
    is Byte -> IntValue(this.toInt())
    is Short -> IntValue(this.toInt())
    is Int -> IntValue(this)
    is Long -> LongValue(this)
    is Boolean -> BooleanValue(this)
    is Float -> FloatValue(this)
    is Double -> DoubleValue(this)
    is Char -> StringValue(this.toString())
    else -> StringValue(this.toString())
}

/**
 * Extracts DRIs from a [PsiReferenceList] (e.g., throws list).
 */
public fun PsiReferenceList.toDriList(): List<DRI> =
    referenceElements.mapNotNull { it?.resolve()?.let { resolved -> DRI.from(resolved) } }

private const val JVM_FIELD_FQ_NAME = "kotlin.jvm.JvmField"

/**
 * Checks if a [PsiField] has the `@JvmField` annotation.
 */
public fun PsiField.hasJvmFieldAnnotation(): Boolean =
    annotations.any { it.qualifiedName == JVM_FIELD_FQ_NAME || it.qualifiedName == "JvmField" }

/**
 * Converts a [PsiAnnotation] to a Dokka [Annotations.Annotation], preserving
 * the original Java annotation class, argument names, and values.
 */
public fun psiAnnotationToDocumentable(psiAnnotation: PsiAnnotation): Annotations.Annotation? {
    val resolvedClass = psiAnnotation.nameReferenceElement?.resolve() as? PsiClass ?: return null
    val dri = DRI.from(resolvedClass)
    val params = psiAnnotation.parameterList.attributes.mapNotNull { attr ->
        val name = attr.name ?: "value"
        val value = attr.value?.let { psiAnnotationValueToDocumentable(it) } ?: return@mapNotNull null
        name to value
    }.toMap()
    val mustBeDocumented = resolvedClass.annotations.any { it.hasQualifiedName("java.lang.annotation.Documented") }
    return Annotations.Annotation(dri = dri, params = params, mustBeDocumented = mustBeDocumented)
}

/**
 * Converts a [PsiAnnotationMemberValue] to a Dokka [AnnotationParameterValue].
 */
public fun psiAnnotationValueToDocumentable(value: PsiAnnotationMemberValue): AnnotationParameterValue? {
    return when (value) {
        is PsiArrayInitializerMemberValue -> ArrayValue(
            value.initializers.mapNotNull { psiAnnotationValueToDocumentable(it) }
        )
        is PsiAnnotation -> psiAnnotationToDocumentable(value)?.let { AnnotationValue(it) }
        is PsiReferenceExpression -> {
            val resolved = value.resolve()
            when {
                resolved is PsiField && resolved.containingClass?.isEnum == true -> {
                    // Match PSI translator behavior: resolve the first child reference
                    // (typically the enum class) rather than the full enum entry
                    val refElement = (value as PsiElement).children
                        .filterIsInstance<PsiJavaCodeReferenceElement>().firstOrNull()?.resolve()
                    val dri = refElement?.let { DRI.from(it) } ?: DRI.from(resolved)
                    EnumValue(value.text ?: "", dri)
                }
                resolved is PsiField -> {
                    // Static constant field: resolve to its computed constant value
                    resolved.computeConstantValue()?.toAnnotationLiteralValue()
                        ?: value.text?.let { StringValue(it) }
                }
                else -> value.text?.let { StringValue(it) }
            }
        }
        is PsiClassObjectAccessExpression -> {
            when (val type = value.operand.type) {
                is PsiClassType -> type.resolve()?.let {
                    ClassValue(it.name ?: "", DRI.from(it))
                }
                is PsiArrayType -> (type.componentType as? PsiClassType)?.resolve()?.let {
                    ClassValue(it.name ?: "", DRI.from(it))
                }
                else -> null
            }
        }
        is PsiLiteralExpression -> when (val v = value.value) {
            is Boolean -> BooleanValue(v)
            is Int -> IntValue(v)
            is Long -> LongValue(v)
            is Float -> FloatValue(v)
            is Double -> DoubleValue(v)
            is String -> StringValue(v)
            is Char -> StringValue(v.toString())
            else -> value.text?.let { StringValue(it) }
        }
        else -> value.text?.let { StringValue(it) }
    }
}
