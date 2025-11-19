/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// links for external libraries
// TODO: id is not enough, as we could have section references
// there should be shortcut if default linking form is used:
// - modules/[module]/api/[package]/[class]/[callable]/index.html
// - modules/[module]/api/[package]/[callable]/index.html
// - modules/[module]/api/[package]/index.html
// - modules/[module]/docs/[path]/[name]/index.html
// - modules/[module]/index.html - module
// - docs/[path]/[name].html
// - package-list (single-module)
// - element-list (multi-module)
// upper case characters is replaced: `HeyX` -> `-hey-x`
//@Serializable
//public data class KdExternalModule(
//    public val links: List<KdExternalLink>
//)
//
//@Serializable
//public data class KdExternalLink(
//    public val id: KdElementId,
//    public val url: String
//)
//
//private class SomeModule(
//    val sourceLinks: Links, // links declaration -> source location (GH)
//    val webLinks: Links,    // links declaration -> website location
//)
//
//private class Links(
//    val links: List<KdExternalLink> = emptyList()
//)

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
