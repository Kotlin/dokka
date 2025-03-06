/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public data class KdAnnotation(
    val annotationId: KdSymbolId,
    val parameters: List<KdAnnotationParameter>
)

public data class KdAnnotationParameter(
    val name: String,
    val type: KdType,
    val value: String,
)
