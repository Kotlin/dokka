/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// TODO: should we add something like `other: JsonObject` here, so that it can be used for custom properties?
//  or, things like, future versions? or it's just fine to update the version of the lib?
@Serializable
public sealed class KdElement : KdDocumented {
    public abstract val name: String
    public abstract val id: KdElementId
}
