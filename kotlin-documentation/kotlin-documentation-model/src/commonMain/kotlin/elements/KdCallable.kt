/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// java field, kotlin property (including synthetic one), enum_entry, function, constructor

@Serializable
public sealed class KdCallable : KdDeclaration() {
    abstract override val id: KdCallableId
    public abstract val returns: KdReturns

    // means, it's explicitly marked as `override`, and not just an inherited callable
    public abstract val isOverride: Boolean
    public abstract val isStatic: Boolean // do nothing until static KEEP?
    public abstract val receiverParameter: KdReceiverParameter?
    public abstract val contextParameters: List<KdContextParameter>
    public abstract val throws: List<KdThrows>

    // there could be multiple overrides - not really "override" more - "inherits from"?
    // TODO: it really should be a KdClassLikeId probably?
    //  TBD should we list all override declarations, or something else
    public abstract val inheritedFrom: List<KdCallableId>
}

