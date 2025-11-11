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
    override val documentation: KdDocumentation? = null,
) : KdDocumented

@Serializable
public data class KdFragment(
    // TODO: the name is correct only for the current module and not comparable between different modules
    val name: String,
    // TODO: decide on this representation, uklibs uses `refines`
    val dependsOn: List<String> = emptyList(),
    // TODO: decide if we need information about platform/target here
    val packages: List<KdPackage> = emptyList(),
    override val documentation: KdDocumentation? = null, // a.k.a module-docs
) : KdDocumented

// TODO: some other metadata could go here from YAML frontmatter ???
@Serializable
public data class KdPackage(
    val name: String,
    val declarations: List<KdDeclaration> = emptyList(),
    override val documentation: KdDocumentation? = null,
) : KdDocumented {
}
