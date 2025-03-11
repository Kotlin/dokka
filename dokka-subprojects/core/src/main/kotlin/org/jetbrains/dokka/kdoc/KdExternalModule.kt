/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public interface KdExternalModule {
//    public val name: String
//    public val version: String

    public val packages: Map<KdSymbolId, KdSymbolLink>
    public val fragments: List<KdExternalFragment>
}

public interface KdExternalFragment {
    public val name: String // common, jvm, native, etc
//    public val platforms: Set<String>
//    public val dependsOn: Set<String>

    public val declarations: Map<KdSymbolId, KdSymbolLink>
}

// TODO: references to web sources (github) and sources.jar could exist?
