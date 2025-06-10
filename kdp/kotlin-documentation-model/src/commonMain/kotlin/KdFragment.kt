/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

// fragment <- from code per source-set
@Serializable
public data class KdFragment(
    // informational data
    // val name: String (jvm, common, js, wasm, web, native, apple, etc) - what about test?
    // val platforms: List (jvm, mingwX64, etc) - duplicate info?
    val declarations: List<KdDeclaration>
)
