/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(kotlin.contracts.ExperimentalContracts::class)
package org.jetbrains.dokka.uitest.kmp.common

import kotlinx.coroutines.CoroutineScope

/**
 * Example function with context receivers that demonstrates Dokka formatting.
 *
 * This function shows how context receivers appear in generated documentation
 * with automatic line wrapping for long parameter lists.
 */
/* **Context receivers:**
* - `s1: String`
* - `s2: String`
* - `s3: String`
* - `s4: String`
* - `s5: String`
* - `s6: String`
* - `s7: String`
* - `s8: String`
* - `s9: String`
* - `s10: String`
*/
context(
    s1: String,
    s2: String,
    s3: String,
    s4: String,
    s5: String,
    s6: String,
    s7: String,
    s8: String,
    s9: String,
    s10: String
)

fun Int.simpleFun(
    a1: String,
    a2: String,
    a3: String,
    a4: String,
    a5: String,
    a6: String,
    a7: String,
    a8: String,
    a9: String,
    a10: String
): String {
    return "Result: $this + $a1" + "$s1"
}