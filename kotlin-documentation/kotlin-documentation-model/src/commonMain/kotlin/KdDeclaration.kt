/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

//@Serializable(KdDeclarationSerializer::class)
@Serializable
public sealed class KdDeclaration : KdDocumented {
    public abstract val name: String

    public abstract val isExternal: Boolean
    public abstract val sourceLanguage: KdSourceLanguage
    public abstract val visibility: KdVisibility
    public abstract val modality: KdModality
    public abstract val actuality: KdActuality?
    public abstract val annotations: List<KdAnnotation>
    public abstract val typeParameters: List<KdTypeParameter>
}
//
//internal object KdDeclarationSerializer : KSerializer<KdDeclaration> {
//    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
//        "KdDeclaration"
//    ) {
//        // TODO: polymorphic
//    }
//
//    override fun serialize(encoder: Encoder, value: KdDeclaration) {
//        encoder.encodeSerializableValue(
//            KdDeclarationWrapper.serializer(),
//            KdDeclarationWrapper(
//                when (value) {
//                    is KdConstructor -> TODO()
//                    is KdFunction -> KdFunctionOneOf(value)
//                    is KdProperty -> TODO()
//                    is KdClass -> TODO()
//                    is KdTypealias -> TODO()
//                }
//            )
//        )
//    }
//
//    override fun deserialize(decoder: Decoder): KdDeclaration {
//        return decoder.decodeSerializableValue(KdDeclarationWrapper.serializer()).value.value
//    }
//}
//
//@Serializable
//@JvmInline
//internal value class KdDeclarationWrapper(
//    @ProtoOneOf val value: KdDeclarationOneOf
//)
//
//@Serializable
//internal sealed interface KdDeclarationOneOf {
//    val value: KdDeclaration
//}
//
////@SerialName("function")
//@Serializable
//@JvmInline
//internal value class KdFunctionOneOf(
//    @ProtoNumber(1) override val value: KdFunction
//) : KdDeclarationOneOf
