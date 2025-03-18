/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public interface KdType


//public interface KdSymbolReference
public interface KdTypeReference {
    // TODO: functional types
    public val isNullable: Boolean
    public val symbolReference: KdSymbolReference
    public val typeParameters: List<
            KdTypeParameter // + star projection
            >
}
