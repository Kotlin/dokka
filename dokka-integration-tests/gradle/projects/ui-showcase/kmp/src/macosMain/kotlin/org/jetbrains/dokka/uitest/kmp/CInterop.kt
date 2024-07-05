/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")

package org.jetbrains.dokka.uitest.kmp

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Low-level MacOS function
 */
@OptIn(ExperimentalForeignApi::class)
fun <T : CPointed> printPointerRawValue(pointer: CPointer<T>) {
    println(pointer.rawValue)
}
