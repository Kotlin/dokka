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

@Serializable(KdPackageIdSerializer::class)
public data class KdPackageId(
    public val packageName: String,
) : KdSymbolId()

internal object KdPackageIdSerializer : KSerializer<KdPackageId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdPackageId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdPackageId) {
        encoder.encodeString(value.packageName)
    }

    override fun deserialize(decoder: Decoder): KdPackageId {
        return KdPackageId(decoder.decodeString())
    }
}

// TODO: some other metadata could go here from YAML frontmatter ???
// on java -> from package-info.java
@Serializable
public data class KdPackage(
    override val id: KdPackageId,
    override val name: String,
    val classifiers: List<KdClassifierId> = emptyList(),
    val callables: List<KdCallableId> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdSymbol()
