/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// we should have access only to `MustBeDocumented` annotations
public data class KdAnnotation(
    val classId: KdDeclarationId,
    val arguments: List<KdAnnotationArgument>
)

public data class KdAnnotationArgument(
    val name: String,
    val value: KdAnnotationArgumentValue,
)

public sealed class KdAnnotationArgumentValue {
    public data class ConstValue(public val value: KdConstValue) : KdAnnotationArgumentValue()

    //value
    //class
    //array
    //etc
}
