/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: check Kotlin spec - it gives a good idea on how to generalize the models!!!

@Serializable
public data class KdClassifierId(
    override val packageName: String,
    public val classNames: String, // it could be A.B.C for nested class
) : KdDeclarationId()

@Serializable
public sealed class KdClassifier : KdDeclaration() {
    abstract override val id: KdClassifierId
    public abstract val typeParameters: List<KdTypeParameter>
}

@SerialName("class")
@Serializable
public data class KdClass(
    override val id: KdClassifierId,

    // TODO: classKind and flags (data, value, inner, companion) interactions
    public val classKind: KdClassKind,

    override val typeParameters: List<KdTypeParameter> = emptyList(),
    public val superTypes: List<KdType> = emptyList(),
    public val declarations: List<KdDeclarationId> = emptyList(),
    public val isExternal: Boolean = false, // TODO?

    public val isCompanion: Boolean = false,
    public val isData: Boolean = false,
    public val isValue: Boolean = false,
    public val isInner: Boolean = false,

    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,

    override val documentation: KdDocumentation? = null,
    override val tags: List<KdTag> = emptyList(),
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdClassifier()

@SerialName("typealias")
@Serializable
public data class KdTypealias(
    override val id: KdClassifierId,

    public val underlyingType: KdType,

    override val typeParameters: List<KdTypeParameter> = emptyList(),

    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val actuality: KdActuality? = null,

    override val annotations: List<KdAnnotation> = emptyList(),
    override val documentation: KdDocumentation? = null,
    override val tags: List<KdTag> = emptyList(),
) : KdClassifier() {
    // TODO: is it true? :)
    override val sourceLanguage: KdSourceLanguage get() = KdSourceLanguage.KOTLIN
    override val modality: KdModality get() = KdModality.FINAL
}
