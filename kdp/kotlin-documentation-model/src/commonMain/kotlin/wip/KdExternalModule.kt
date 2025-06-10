/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// links for external libraries
// TODO: id is not enough, as we could have section references
@Serializable
public data class KdExternalModule(
    public val links: List<KdExternalLink>
)

@Serializable
public data class KdExternalLink(
    public val id: KdElementId,
    public val url: String
)

//public interface KdExternalModule {
////    public val name: String
////    public val version: String
//
//    public val packages: Map<KdDeclarationId, KdSymbolLink>
//    public val fragments: List<KdExternalFragment>
//}
//
//public interface KdExternalFragment {
//    public val name: String // common, jvm, native, etc
////    public val platforms: Set<String>
////    public val dependsOn: Set<String>
//
//    public val declarations: Map<KdDeclarationId, KdSymbolLink>
//}

// to generate HTML: local modules (sources) + external modules (links)
