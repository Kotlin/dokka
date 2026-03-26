/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.documentation.KdAnnotation
import org.jetbrains.kotlin.documentation.KdAnnotationArgument
import org.jetbrains.kotlin.documentation.KdAnnotationArgumentValue
import org.jetbrains.kotlin.documentation.KdConstValue

internal fun Annotations.Annotation.toKdAnnotation(
    mustBeDocumented: Boolean = this.mustBeDocumented,
): KdAnnotation? {
    if (!mustBeDocumented) return null

    fun AnnotationParameterValue.toKdAnnotationArgumentValue(): KdAnnotationArgumentValue {
        return when (this) {
            is AnnotationValue -> KdAnnotationArgumentValue.Annotation(annotation.toKdAnnotation(mustBeDocumented = true)!!)
            is ArrayValue -> KdAnnotationArgumentValue.Array(value.map { it.toKdAnnotationArgumentValue() })
            is ClassValue -> KdAnnotationArgumentValue.Class(classDRI.toKdClassifierId())
            is EnumValue -> KdAnnotationArgumentValue.Enum(enumDri.toKdCallableId())
            // TODO
            is LiteralValue -> KdAnnotationArgumentValue.Const(KdConstValue(text()))
        }
    }

    return KdAnnotation(
        classifierId = dri.toKdClassifierId(),
        useSiteTargets = emptyList(), // TODO
        arguments = params.map { (key, value) ->
            KdAnnotationArgument(
                name = key,
                value = value.toKdAnnotationArgumentValue()
            )
        }
    )
}