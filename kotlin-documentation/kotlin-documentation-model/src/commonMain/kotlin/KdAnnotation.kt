/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public sealed interface KdAnnotated {
    public val annotations: List<KdAnnotation>
}

// we should have access only to `MustBeDocumented` annotations
@Serializable
public data class KdAnnotation(
    val classifierId: KdClassifierId,
    val useSiteTargets: List<KdAnnotationUseSiteTarget>, // TODO: non-empty-list?
    val arguments: List<KdAnnotationArgument> = emptyList(),
)

// TODO: take from AA? TBD
public enum class KdAnnotationUseSiteTarget {
    FILE,
    PROPERTY,
    FIELD,
    GET,
    SET,
    RECEIVER,
    PARAM,
    SETPARAM,
    DELEGATE
}

@Serializable
public data class KdAnnotationArgument(
    val name: String,
    val value: KdAnnotationArgumentValue,
)

@Serializable
public sealed class KdAnnotationArgumentValue {
    @SerialName("const")
    @Serializable
    public data class Const(public val value: KdConstValue) : KdAnnotationArgumentValue()

    // TODO: should it be classifier + name?
    // TODO: enum-value
    @SerialName("enum")
    @Serializable
    public data class Enum(public val callableId: KdCallableId) : KdAnnotationArgumentValue()

    @SerialName("class")
    @Serializable
    public data class Class(public val classifierId: KdClassifierId) : KdAnnotationArgumentValue()

    @SerialName("annotation")
    @Serializable
    public data class Annotation(public val annotation: KdAnnotation) : KdAnnotationArgumentValue()

    @SerialName("array")
    @Serializable
    public data class Array(public val elements: List<KdAnnotationArgumentValue>) : KdAnnotationArgumentValue()

    //value
    //class
    //array
    //etc
}
