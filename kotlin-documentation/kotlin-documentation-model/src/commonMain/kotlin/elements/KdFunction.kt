/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: do the same as for class
public enum class KdFunctionKind {
    PRIMARY_CONSTRUCTOR, CONSTRUCTOR, FUNCTION
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
    override val id: KdCallableId,
    override val name: String,
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
    override val overrides: List<KdCallableOverride>,
    override val source: KdSource = KdSource.KOTLIN,
    override val visibility: KdVisibility = KdVisibility.PUBLIC,
    override val modality: KdModality = KdModality.FINAL,
    override val actuality: KdActuality? = null,
    override val isExternal: Boolean = false,
    override val annotations: List<KdAnnotation> = emptyList(),
    override val typeParameters: List<KdTypeParameter> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdCallable()