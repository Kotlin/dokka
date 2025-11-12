/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")

package it.android

import android.content.Context
import android.util.SparseIntArray
import android.view.View

/**
 * This class is specific to android and uses android classes like:
 * [Context], [SparseIntArray] or [View]
 */
class AndroidSpecificClass {
    fun sparseIntArray() = SparseIntArray()
    fun createView(context: Context): View = View(context)
}
