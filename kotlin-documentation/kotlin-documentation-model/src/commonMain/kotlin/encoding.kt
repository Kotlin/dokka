/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    classDiscriminator = "kind"
}

public fun KdModule.encodeToJson(prettyPrint: Boolean): String {
    val json = if (prettyPrint) prettyJson else Json.Default
    return json.encodeToString(KdModule.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun KdModule.encodeToProtoBuf(): ByteArray {
    return ProtoBuf.encodeToByteArray(KdModule.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun KdModule.encodeToCbor(): ByteArray {
    return Cbor.encodeToByteArray(KdModule.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun protoSchema(): String {
    return ProtoBufSchemaGenerator.generateSchemaText(KdModule.serializer().descriptor)
}
