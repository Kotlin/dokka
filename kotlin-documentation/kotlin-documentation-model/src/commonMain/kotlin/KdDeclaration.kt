/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public sealed class KdDeclaration : KdDocumented {
    public abstract val name: String

    public abstract val isExternal: Boolean
    public abstract val sourceLanguage: KdSourceLanguage
    public abstract val visibility: KdVisibility
    public abstract val modality: KdModality
    public abstract val actuality: KdActuality?
    public abstract val annotations: List<KdAnnotation>
    public abstract val typeParameters: List<KdTypeParameter>
}
