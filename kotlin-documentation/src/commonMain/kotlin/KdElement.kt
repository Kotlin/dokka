/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

@Serializable
public sealed class KdElementId

@Serializable
public sealed class KdElement : KdDocumented() {
    public abstract val id: KdElementId
}
