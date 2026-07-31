/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// those are ROOT types

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


// fragment = main (target=jvm)

// we work on the level of the module for now
// project(kotlinx.coroutines) ->        <- build system only
//  module(coroutines-core) ->
//   fragment(commonMain) ->
//    package(kotlinx.coroutines) -> | can be shared between fragments
//     [file(Job.kt)] ->
//      class(Job) ->               | can be shared between fragments - expect/actual
//       declaration(cancel)        | can be shared between fragments - expect/actual


// TODO: the name is correct only for the current module (dependsOn) and not comparable between different modules
@Serializable
public data class KdFragment(
    val name: String,
    // TODO: do we need it here, or we should embed psm as a separate file?
    val dependsOn: List<String> = emptyList(),
    val elements: List<KdElement> = emptyList(),
)

// TODO: we might want to also represent android flavors in a similar way, so maybe `fragment` is not the best name
@Serializable // output of an analyzer
public data class KdFragments(
    val fragments: List<KdFragment> = emptyList()
)
