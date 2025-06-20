/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public sealed class KdDeclaration : KdElement() {
    abstract override val id: KdDeclarationId
    // public abstract val fileId: KdFileId

    public abstract val sourceLanguage: KdSourceLanguage
    public abstract val visibility: KdVisibility
    public abstract val modality: KdModality
    public abstract val actuality: KdActuality?

    public abstract val documentationTags: List<KdDocumentationTag>
    public abstract val annotations: List<KdAnnotation>
}

public class KdSourceFileInformation(
    public val path: String, // path to file
    public val language: KdSourceLanguage,
    public val declarations: List<KdDeclarationId>
)
