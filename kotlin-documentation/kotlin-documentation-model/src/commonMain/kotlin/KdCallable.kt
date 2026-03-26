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

// java field, kotlin property (including synthetic one), enum_entry, function, constructor

// represents: constructor (no name), function, property, enum_entry, java field
@Serializable(KdCallableIdSerializer::class)
public data class KdCallableId(
    public val packageName: String,
    public val classNames: String?, // if null -> top-level
    // TODO: how to distinguish between: constructor vs function, property vs function
    public val callableName: String?, // if null -> constructor, `classNames` should be not null
//    public val isProperty: Boolean // if false -> function - TODO: should we?
)

internal object KdCallableIdSerializer : KSerializer<KdCallableId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KdCallableId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KdCallableId) {
        encoder.encodeString(value.packageName + "/" + value.classNames.orEmpty() + "/" + value.callableName.orEmpty())
    }

    override fun deserialize(decoder: Decoder): KdCallableId {
        val parts = decoder.decodeString().split('/')
        require(parts.size == 3) { "ClassifierId should be a pair of package and class names" }
        return KdCallableId(parts[0], parts[1], parts[2])
    }
}

@Serializable
public sealed class KdCallable : KdDeclaration() {
    public abstract val returns: KdReturns

    public abstract val isStatic: Boolean // do nothing until static KEEP?
    public abstract val receiverParameter: KdReceiverParameter?
    public abstract val contextParameters: List<KdContextParameter>
    public abstract val throws: List<KdThrows>
}

@SerialName("constructor")
@Serializable
public data class KdConstructor(
    override val name: String,
    override val returns: KdReturns, // TODO is it fine?
    // optionals
    val isPrimary: Boolean = false,
    val valueParameters: List<KdValueParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val documentation: List<KdDocumentationNode>,
    override val annotations: List<KdAnnotation> = emptyList(),
) : KdCallable() {
    override val isStatic: Boolean get() = false
    override val typeParameters: List<KdTypeParameter> get() = emptyList()
    override val receiverParameter: KdReceiverParameter? get() = null
    override val contextParameters: List<KdContextParameter> get() = emptyList()
}

/*
// common
expect abstract class A {
  fun accepts(b: B)
}
expect class B

class CommonImpl: A {
  override fun accepts(b: B)
}

// jvm
actual typealias A = PlatformA
actual typealias B = String
class JvmImpl: A {
  override fun accepts(b: B)
}
// native
actual abstract class A {
  actual fun accepts(b: B)
}
actual class B
class NativeImpl: A {
  override fun accepts(b: B)
}

what should be the relations in this case + when it's about libraries
 */

// TODO: how to handle override relations and expect/actual relations
//  for this, there should be some specific references
//  because `overload` resolution is rather complex concept
@SerialName("function")
@Serializable
public data class KdFunction(
    override val name: String, // TODO: what is the name for constructors? `<init>` or ``(empty-string) or class-name?
    override val returns: KdReturns,
    // optionals
    val isSuspend: Boolean = false,
    val isOperator: Boolean = false,
    val isInfix: Boolean = false,
    val isInline: Boolean = false,
    val isTailRec: Boolean = false,
    override val isStatic: Boolean = false,
    override val receiverParameter: KdReceiverParameter? = null,
    val valueParameters: List<KdValueParameter> = emptyList(),
    override val contextParameters: List<KdContextParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdCallable()

// getter and setter could have different visibility, so we should have them? they could also have annotations
// we can't really document getter or setter explicitly
// java synthetic property can have field + get/set. Kotlin with EBH also can have different field type?
// what to do with java field+getField+setField based on visibilities
@SerialName("variable")
@Serializable
public data class KdVariable(
    override val name: String,
    override val returns: KdReturns,
    val variableKind: KdVariableKind,
    // optionals
    val isMutable: Boolean = false, // isVar or isVal
    val constValue: KdConstValue? = null,
    // TODO: getter and setter? do we need them?
    override val isStatic: Boolean = false,
    override val receiverParameter: KdReceiverParameter? = null,
    override val contextParameters: List<KdContextParameter> = emptyList(),
    override val throws: List<KdThrows> = emptyList(),
    override val sourceLanguage: KdSourceLanguage = KdSourceLanguage.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdCallable()