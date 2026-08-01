/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// referencable elements
// TBD if we need this, but it feels like should be a prefix of a string
public enum class KdElementKind {
    MODULE, // impossible in Kotlin, but possible in Java
    PACKAGE,
    CLASS, // object, class, typealias, etc
    CALLABLE,
    // PROPERTY,
    // CONSTRUCTOR,
    // TODO: statics and companion things?
}

// an idea on how the link could look like in text representation

// links to declarations/symbols - or KdDeclarationId?
//
//  package:PACKAGE_NAME
//    class:PACKAGE_NAME/CLASS_NAME
// constructor:PACKAGE_NAME/          /PROPERTY_NAME/HASH
// property:PACKAGE_NAME/          /PROPERTY_NAME/HASH
// function:PACKAGE_NAME/          /FUNCTION_NAME/HASH
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/HASH
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME#HASH

// variable:PACKAGE_NAME/CLASS_NAME/PROPERTY_NAME/0
// variable:PACKAGE_NAME/CLASS_NAME/PROPERTY_NAME/1
// variable:PACKAGE_NAME/CLASS_NAME/ENUM_ENTRY_NAME/0
// function:PACKAGE_NAME/CLASS_NAME//0
// function:PACKAGE_NAME/CLASS_NAME//1
// function:PACKAGE_NAME/CLASS_NAME//2
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/0
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/1
// variable:PACKAGE_NAME/CLASS_NAME/ENUM_ENTRY_NAME/XXX

// in future, we might want to add more `ids`, like, for example `sample` or `topic`

@Serializable(KdElementIdSerializer::class)
public sealed class KdElementId

@Serializable(KdDeclarationIdSerializer::class)
public sealed class KdDeclarationId : KdElementId()

// TODO: clashes?
@Serializable(KdModuleIdSerializer::class)
public data class KdModuleId(
    public val moduleName: String,
) : KdElementId()

@Serializable(KdPackageIdSerializer::class)
public data class KdPackageId(
    public val packageName: String,
) : KdElementId()

// class or typealias
@Serializable(KdClassLikeIdSerializer::class)
public data class KdClassLikeId(
    public val packageName: String,
    public val classNames: String, // it could be A.B.C for nested class
) : KdDeclarationId()

// represents: constructor (no name), function, property, enum_entry, java field
@Serializable(KdCallableIdSerializer::class)
public data class KdCallableId(
    public val packageName: String,
    public val classNames: String?, // if null -> top-level
    // TODO: how to distinguish between: constructor vs function, property vs function
    public val callableName: String?, // if null -> constructor, `classNames` should be not null
//    public val isProperty: Boolean // if false -> function - TODO: should we?
    public val hash: String
) : KdDeclarationId()

// serializers

internal object KdElementIdSerializer : KSerializer<KdElementId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdElementId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdElementId) {
        when (value) {
            is KdCallableId -> KdCallableIdSerializer.serialize(encoder, value)
            is KdClassLikeId -> KdClassLikeIdSerializer.serialize(encoder, value)
            is KdPackageId -> KdPackageIdSerializer.serialize(encoder, value)
            is KdModuleId -> KdModuleIdSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): KdElementId {
        val id = decoder.decodeString()
        val firstParts = id.split(':')
        require(firstParts.size == 2) { "wrong format: $id" }

        val name = firstParts[1]
        return when (KdElementKind.valueOf(firstParts[0])) {
            KdElementKind.MODULE -> KdModuleId(name)
            KdElementKind.PACKAGE -> KdPackageId(name)
            KdElementKind.CLASS -> {
                val parts = name.split('/')
                require(parts.size == 2) { "classLikeId should be a pair of package and class names" }
                KdClassLikeId(parts[0], parts[1])
            }

            KdElementKind.CALLABLE -> {
                val parts = name.split('/')
                require(parts.size == 4) { "classLikeId should be a pair of package and class names" }
                KdCallableId(parts[0], parts[1], parts[2], parts[3])
            }
        }
    }
}

internal object KdDeclarationIdSerializer : KSerializer<KdDeclarationId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdDeclarationId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdDeclarationId) {
        when (value) {
            is KdClassLikeId -> KdClassLikeIdSerializer.serialize(encoder, value)
            is KdCallableId -> KdCallableIdSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): KdDeclarationId {
        val id = KdElementIdSerializer.deserialize(decoder)
        return id as? KdDeclarationId ?: error("Not a declaration id: $id")
    }
}

internal object KdModuleIdSerializer : KSerializer<KdModuleId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdModuleId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdModuleId) {
        encoder.encodeString(
            "${KdElementKind.MODULE.name}:${value.moduleName}"
        )
    }

    override fun deserialize(decoder: Decoder): KdModuleId {
        val id = KdElementIdSerializer.deserialize(decoder)
        return id as? KdModuleId ?: error("Not a module id: $id")
    }
}

internal object KdPackageIdSerializer : KSerializer<KdPackageId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdPackageId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdPackageId) {
        encoder.encodeString(
            "${KdElementKind.PACKAGE.name}:${value.packageName}"
        )
    }

    override fun deserialize(decoder: Decoder): KdPackageId {
        val id = KdElementIdSerializer.deserialize(decoder)
        return id as? KdPackageId ?: error("Not a package id: $id")
    }
}

internal object KdClassLikeIdSerializer : KSerializer<KdClassLikeId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdclassLikeId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdClassLikeId) {
        encoder.encodeString(
            "${KdElementKind.CLASS.name}:${value.packageName}/${value.classNames}"
        )
    }

    override fun deserialize(decoder: Decoder): KdClassLikeId {
        val id = KdElementIdSerializer.deserialize(decoder)
        return id as? KdClassLikeId ?: error("Not a classlike id: $id")
    }
}

internal object KdCallableIdSerializer : KSerializer<KdCallableId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdCallableId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdCallableId) {
        encoder.encodeString(
            "${KdElementKind.CALLABLE.name}:${value.packageName}/${value.classNames.orEmpty()}/${value.callableName.orEmpty()}/${value.hash}"
        )
    }

    override fun deserialize(decoder: Decoder): KdCallableId {
        val id = KdElementIdSerializer.deserialize(decoder)
        return id as? KdCallableId ?: error("Not a callable id: $id")
    }
}
