/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// TODO: kind vs separate class vs flags - take a look on kotlin spec
// TODO: probably replace those `kinds` with separate classes, so that for the json consumer, all entities will be represented as single `type` field in json

public enum class KdClassKind {
    CLASS, ENUM_CLASS, ANNOTATION_CLASS, OBJECT, INTERFACE,

    JAVA_RECORD // ???
}


// TODO: decide on what to do with `expect class`/`actual typealias` in regard to declarations available in `class`

// TODO: split into: KdObject, KdEnumClass, KdAnnotationClass, KdRecord, KdInterface ???
// inner class, // separate thing, only for classes
@SerialName("class")
@Serializable
public data class KdClass(
    override val id: KdClassLikeId,
    override val name: String,
    // TODO: consult frontend metadata
    // TODO: check Kotlin spec - it gives a good idea on how to generalize the models!!!
    // TODO: decide how to represent this
    // TODO: classKind and flags (data, value, inner, companion) interactions
    val classKind: KdClassKind,

    val isCompanion: Boolean = false,
    val isData: Boolean = false,
    val isValue: Boolean = false,
    val isFun: Boolean = false,

    val isInner: Boolean = false,
    val superTypes: List<KdType> = emptyList(),
    val constructors: List<KdCallableId> = emptyList(), // TODO: do we need this?
    val callables: List<KdCallableId> = emptyList(),
    val classlikes: List<KdClassLikeId> = emptyList(),

    // TODO: do we need to show both inherited and overridden callables? - yes
    // TODO: what should happen if we had override, but then removed it, or vice-versa, added it - it should continue to work
    // it looks like we need separate lists for them
    // those are callables, which are coming from parent classes, but not overridden
    // TODO: it could be coming from java class...
    //  looks like in this case we need to still include it + have some reference, that it's just inherited?
    //  e.g it could be [A.x] where x is declared in B, and A: B
    //  if we are working with kotlin->kotlin where both have KDM generated - there is no need for this
    // TODO: it's very similar to expect/actual case
    // TODO: having all "inherited" callables here might require more post-processing if we already have KDM model for external declarations from libraries
    //  maybe we need to somehow split those more naturally in the list here and in the doc?
    //  e.g. here in class A we could have:
    //  - example/A/hashCode - inherits [kotlin/Any/hashCode, example/B/hashCode]
    //  - example/A/toString - overrides kotlin/Any/toString (override + maybe add docs (or not))

    // for Java:
    // - if we need to include "inherited" members -> Java supertypes is a must
    // - if we need to include only "declared" members -> Java supertypes is not needed

    override val source: KdSource = KdSource.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdClassLike()
