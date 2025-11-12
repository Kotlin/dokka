/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

public fun KdModule.encodeToJson(prettyPrint: Boolean): String {
    val json = if (prettyPrint) prettyJson else Json.Default
    return json.encodeToString(KdModule.serializer(), this)
}

@OptIn(ExperimentalSerializationApi::class)
public fun KdModule.encodeToProtoBuf(): ByteArray {
    return ProtoBuf.encodeToByteArray(KdModule.serializer(), this)
}
