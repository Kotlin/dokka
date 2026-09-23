/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

// The canonical class discriminator is `kind`, for both the compact and the pretty form.
private val compactJson = Json {
    classDiscriminator = "kind"
}

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    classDiscriminator = "kind"
}

public fun KdFragments.encodeToJson(prettyPrint: Boolean): String {
    val json = if (prettyPrint) prettyJson else compactJson
    return json.encodeToString(KdFragments.serializer(), this)
}
public fun KdFragment.encodeToJson(prettyPrint: Boolean): String {
    val json = if (prettyPrint) prettyJson else compactJson
    return json.encodeToString(KdFragment.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun KdFragments.encodeToProtoBuf(): ByteArray {
    return ProtoBuf.encodeToByteArray(KdFragments.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun KdFragments.encodeToCbor(): ByteArray {
    return Cbor.encodeToByteArray(KdFragments.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun protoSchema(): String {
    return ProtoBufSchemaGenerator.generateSchemaText(KdFragments.serializer().descriptor)
}
