/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public sealed class KdElement : KdDocumented {
    public abstract val name: String
    public abstract val id: KdElementId
}
