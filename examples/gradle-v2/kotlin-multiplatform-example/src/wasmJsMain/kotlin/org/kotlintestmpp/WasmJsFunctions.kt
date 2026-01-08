/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.kotlintestmpp

/**
 * Function declared in WasmJS source set
 */
fun wasmJs() {}

/**
 * Function declared in WasmJS source set.
 *
 * Function with the same name exists in another source set as well.
 */
fun shared() {}

/**
 * Extension declared in WasmJS source set
 */
fun String.myExtension() = println("test")
