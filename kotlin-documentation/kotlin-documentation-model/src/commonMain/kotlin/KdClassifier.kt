/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(KdClassifierIdSerializer::class)
public data class KdClassifierId(
    public val packageName: String,
    public val classNames: String, // it could be A.B.C for nested class
) : KdSymbolId()

internal object KdClassifierIdSerializer : KSerializer<KdClassifierId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdClassifierId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdClassifierId) {
        encoder.encodeString(value.packageName + "/" + value.classNames)
    }

    override fun deserialize(decoder: Decoder): KdClassifierId {
        val parts = decoder.decodeString().split('/')
        require(parts.size == 2) { "ClassifierId should be a pair of package and class names" }
        return KdClassifierId(parts[0], parts[1])
    }
}

@Serializable
public sealed class KdClassifier : KdDeclaration() {
    abstract override val id: KdClassifierId
}

// TODO: enum is one more case? or we could represent entries as `static` variables? valueOf is `static` function, entries is `static` variable

// class can have `declarations` inside, typealias - just typealias
// all class kinds can have declarations inside
// class, interface, object
// enum class
// annotation class
// data class, data object
// value class, value object,
// companion object
// record (java)


// TODO: split into: KdObject, KdEnumClass, KdAnnotationClass, KdRecord, KdInterface ???
// inner class, // separate thing, only for classes
@SerialName("class")
@Serializable
public data class KdClass(
    override val id: KdClassifierId,
    override val name: String,
    // TODO: check Kotlin spec - it gives a good idea on how to generalize the models!!!
    // TODO: decide how to represent this
    // TODO: classKind and flags (data, value, inner, companion) interactions
    val classKind: KdClassKind,

    val isCompanion: Boolean = false,
    val isData: Boolean = false,
    val isValue: Boolean = false,

    val isInner: Boolean = false,
    val superTypes: List<KdType> = emptyList(),
    val classifiers: List<KdClassifierId> = emptyList(),

    // TODO: do we need to show both inherited and overridden callables?
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

    val callables: List<KdCallableId> = emptyList(),
    // TODO: should we have `constructors`?
    // TODO: should we split callables and classifiers?
    override val source: KdSource = KdSource.Kotlin,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdClassifier()

@SerialName("typealias")
@Serializable
public data class KdTypealias(
    override val id: KdClassifierId,
    override val name: String,
    val underlyingType: KdType,
    // optionals
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val actuality: KdActuality? = null,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdClassifier() {
    // TODO: is it true? :)
    override val source: KdSource get() = KdSource.Kotlin
    override val modality: KdModality get() = KdModality.FINAL
    override val isExternal: Boolean get() = false
}
