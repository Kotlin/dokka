/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// java field, kotlin property (including synthetic one), enum_entry, function, constructor

@Serializable
public sealed class KdCallable : KdDeclaration() {
    abstract override val id: KdCallableId

    // TODO: `isCompanion` + `isCompanionExtension`? - TBD
    //  think also about Java here
    public abstract val isCompanion: Boolean

    public abstract val returns: KdReturns
    public abstract val receiverParameter: KdReceiverParameter?
    public abstract val contextParameters: List<KdContextParameter>
    public abstract val throws: List<KdThrows>

    // TODO: should we list all override declarations, or something else
    //public abstract val overrides: List<KdCallableOverride>

}

@Serializable
public data class KdCallableOverride(
    val overrides: KdCallableId,
    val kind: KdCallableOverrideKind
)

public enum class KdCallableOverrideKind {
    DIRECT_OVERRIDE,
    INHERITED // just inherited, not mentioned in class
}

private interface A {
    fun foo()
}

private interface B {
    fun foo()
}

// overrides = [A.foo=direct, B.foo=direct]
private class C : A, B {
    override fun foo() {
        TODO("Not yet implemented")
    }
}

private abstract class B2 {
    fun foo() {}
}

// overrides = [B2.foo=implicit?, A.foo=implicit]
private class C2 : B2(), A

private abstract class B3 : B {
    override fun foo() {
        TODO("Not yet implemented")
    }
}

// overrides = [B3.foo=implicit?, A.foo=implicit]
private class C3 : B3(), A