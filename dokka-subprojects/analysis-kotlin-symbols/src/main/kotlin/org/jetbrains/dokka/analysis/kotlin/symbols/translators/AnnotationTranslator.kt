/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withEnumEntryExtra
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

/**
 * Map [KtAnnotationApplication] to Dokka [Annotations.Annotation]
 */
internal class AnnotationTranslator {
    private fun KaSession.getFileLevelAnnotationsFrom(symbol: KaSymbol) =
        if (symbol.origin != KaSymbolOrigin.SOURCE)
            null
        else
            (symbol.psi?.containingFile as? KtFile)?.symbol?.annotations
                ?.map { toDokkaAnnotation(it) }

    private fun KaSession.getDirectAnnotationsFrom(annotated: KaAnnotated) =
        annotated.annotations.map { toDokkaAnnotation(it) }

    /**
     * The examples of annotations from backing field are [JvmField], [JvmSynthetic].
     *
     * @return direct annotations, annotations from backing field and file-level annotations
     */
    fun KaSession.getAllAnnotationsFrom(annotated: KaAnnotated): List<Annotations.Annotation> {
        val directAnnotations = getDirectAnnotationsFrom(annotated)
        val backingFieldAnnotations =
            (annotated as? KaPropertySymbol)?.backingFieldSymbol?.let { getDirectAnnotationsFrom(it) }.orEmpty()
        val fileLevelAnnotations = (annotated as? KaSymbol)?.let { getFileLevelAnnotationsFrom(it) }.orEmpty()
        return directAnnotations + backingFieldAnnotations + fileLevelAnnotations
    }

    private fun KaAnnotation.isNoExistedInSource() = psi == null
    private fun AnnotationUseSiteTarget.toDokkaAnnotationScope(): Annotations.AnnotationScope = when (this) {
        AnnotationUseSiteTarget.PROPERTY_GETTER -> Annotations.AnnotationScope.DIRECT // due to compatibility with Dokka K1
        AnnotationUseSiteTarget.PROPERTY_SETTER -> Annotations.AnnotationScope.DIRECT // due to compatibility with Dokka K1
        AnnotationUseSiteTarget.FILE -> Annotations.AnnotationScope.FILE
        else -> Annotations.AnnotationScope.DIRECT
    }

    private fun KaSession.mustBeDocumented(annotationApplication: KaAnnotation): Boolean {
        if (annotationApplication.isNoExistedInSource()) return false
        val annotationClass = findClass(annotationApplication.classId ?: return false)
        return annotationClass?.let { mustBeDocumentedAnnotation in it.annotations }
            ?: false
    }

    private fun KaSession.toDokkaAnnotation(annotationApplication: KaAnnotation) =
        Annotations.Annotation(
            dri = annotationApplication.classId?.createDRI()
                ?: DRI(packageName = "", classNames = ERROR_CLASS_NAME), // classId might be null on a non-existing annotation call,
            params = annotationApplication.arguments.associate {
                it.name.asString() to toDokkaAnnotationValue(
                    it.expression
                )
            },
            mustBeDocumented = mustBeDocumented(annotationApplication),
            scope = annotationApplication.useSiteTarget?.toDokkaAnnotationScope() ?: Annotations.AnnotationScope.DIRECT
        )

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun KaSession.toDokkaAnnotationValue(annotationValue: KaAnnotationValue): AnnotationParameterValue =
        when (annotationValue) {
            is KaAnnotationValue.ConstantValue -> {
                when (val value = annotationValue.value) {
                    is KaConstantValue.NullValue -> NullValue
                    is KaConstantValue.FloatValue -> FloatValue(value.value)
                    is KaConstantValue.DoubleValue -> DoubleValue(value.value)
                    is KaConstantValue.LongValue -> LongValue(value.value)
                    is KaConstantValue.IntValue -> IntValue(value.value)
                    is KaConstantValue.BooleanValue -> BooleanValue(value.value)
                    is KaConstantValue.ByteValue -> IntValue(value.value.toInt())
                    is KaConstantValue.CharValue -> StringValue(value.value.toString())
                    is KaConstantValue.ErrorValue -> StringValue(value.render())
                    is KaConstantValue.ShortValue -> IntValue(value.value.toInt())
                    is KaConstantValue.StringValue -> StringValue(value.value)
                    is KaConstantValue.UByteValue -> IntValue(value.value.toInt())
                    is KaConstantValue.UIntValue -> IntValue(value.value.toInt())
                    is KaConstantValue.ULongValue -> LongValue(value.value.toLong())
                    is KaConstantValue.UShortValue -> IntValue(value.value.toInt())
                }
            }

            is KaAnnotationValue.EnumEntryValue -> EnumValue(
                with(annotationValue.callableId) { this?.className?.asString() + "." + this?.callableName?.asString() },
                getDRIFrom(annotationValue)
            )

            is KaAnnotationValue.ArrayValue -> ArrayValue(annotationValue.values.map { toDokkaAnnotationValue(it) })
            is KaAnnotationValue.NestedAnnotationValue -> AnnotationValue(toDokkaAnnotation(annotationValue.annotation))
            is KaAnnotationValue.ClassLiteralValue -> when (val type: KaType = annotationValue.type) {
                is KaClassType -> ClassValue(
                    type.classId.relativeClassName.asString(),
                    type.classId.createDRI()
                )
                else -> ClassValue(
                    type.toString(),
                    DRI(packageName = "", classNames = ERROR_CLASS_NAME)
                )
            }
            is KaAnnotationValue.UnsupportedValue -> ClassValue(
                "<Unsupported Annotation Value>",
                DRI(packageName = "", classNames = ERROR_CLASS_NAME)
            )
        }

    private fun getDRIFrom(enumEntry: KaAnnotationValue.EnumEntryValue): DRI {
        val callableId =
            enumEntry.callableId ?: throw IllegalStateException("Can't get `callableId` for enum entry from annotation")
        return DRI(
            packageName = callableId.packageName.asString(),
            classNames = callableId.className?.asString() + "." + callableId.callableName.asString(),
        ).withEnumEntryExtra()
    }

    companion object {
        val mustBeDocumentedAnnotation = ClassId(FqName("kotlin.annotation"), FqName("MustBeDocumented"), false)
        private val parameterNameAnnotation = ClassId(FqName("kotlin"), FqName("ParameterName"), false)

        /**
         * Functional types can have **generated** [ParameterName] annotation
         * @see ParameterName
         */
        internal fun KaAnnotated.getPresentableName(): String? =
            this.annotations[parameterNameAnnotation]
                .firstOrNull()?.arguments?.firstOrNull { it.name == Name.identifier("name") }?.expression?.let { it as? KaAnnotationValue.ConstantValue }
                ?.let { it.value.value.toString() }
    }
}
