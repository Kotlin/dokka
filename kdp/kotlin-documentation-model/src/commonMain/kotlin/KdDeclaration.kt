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

    // isExpect/isActual
    // but when we publish those, names of fragments become relevant only inside one built model,
    // as it's possible to have different ones, e.g `linux+macos`, in different libraries/projects
    // so fragments (as in uklibs) are represented more based on targets/platforms?
    //
    // "expectFragmentId": "commonMain"
    // "actualFragments": ["jvmMain": {...}, "nativeMain": {...}]
}

public class KdSourceFileInformation(
    public val path: String, // path to file
    public val language: KdSourceLanguage,
    public val declarations: List<KdDeclarationId>
)
