/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// we work on the level of the module for now
// project(kotlinx.coroutines) ->        <- build system only
//  module(coroutines-core) ->
//   fragment(commonMain) ->
//    package(kotlinx.coroutines) -> | can be shared between fragments
//     [file(Job.kt)] ->
//      class(Job) ->               | can be shared between fragments - expect/actual
//       declaration(cancel)        | can be shared between fragments - expect/actual

@Serializable // output of the analyzer
public data class KdModule(
    val name: String,
    val fragments: List<KdFragment> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdDocumented

/**
 * Why fragment as a container, and not part of the declaration model?
 * - expect-actual with typealias means `actual` and `expect` can have different models
 * - expect-actual for classes is not stable yet and it's not clear how will it evolve
 * - this is more in line how compiler represents declarations (by fragments/source-sets)
 * - in case of android flavors (or similar) we can generate fragments at once (if needed) and then produce different HTMl outputs just by filtering fragments, and not transforming the whole model
 * - to not have clashes in case we have same named function/property/class in different source-sets (not expect-actual)
 * - if we want to still have `targets/dependsOn` information, it still should be stored somewhere once
 *
 * Maybe it will be a bad idea in the end :)
 */
@Serializable
public data class KdFragment(
    // TODO: the name is correct only for the current module (dependsOn) and not comparable between different modules
    val name: String,
    val dependsOn: List<String> = emptyList(), // TODO: what about test & main ?
    val targets: List<KdTarget> = emptyList(), // all targets supported by this fragment?
    val packages: List<KdPackage> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(), // a.k.a module-docs
) : KdDocumented

// TODO: some other metadata could go here from YAML frontmatter ???
@Serializable
public data class KdPackage(
    val name: String,
    val declarations: List<KdDeclaration> = emptyList(),
    override val documentation: List<KdDocumentationNode> = emptyList(),
) : KdDocumented {
}
