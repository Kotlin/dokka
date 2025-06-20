/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// class : org.example/ClassA - id
// sources.jar: org/example | ClassA.kt - filepath (+ line number)
// github: github.com/whyoleg/example | module-name/src/commonMain/kotlin/org/example | ClassA.kt - URL (+ line number)
// external: klibs.io/whyoleg/example/org.example/ClassA

// module(XXX) -> fragment(commonMain) -> package(org.example) -> file(Hell.kt) -> class -> declaration

//@Serializable
//public data class KdFragmentId(
//    public val name: String
//) : KdElementId()

// fragment <- from code per KGP source-set
@Serializable
public data class KdFragment(
//    override val id: KdFragmentId,
    // informational data
    // val platforms: List (jvm, mingwX64, etc) - duplicate info?
    val documentation: KdDocumentation? = null, // a.k.a module-docs
    public val packages: List<KdPackage> = emptyList(),
//    public val topics: List<KdTopic> = emptyList() // in future
    // val samples
//    val files: List<KdFile>,
) //: KdElement()
