@file:Suppress("unused")

package org.kotlintestmpp

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer

/**
 * Low-level Linux function
 */
fun <T : CPointed> printPointerRawValue(pointer: CPointer<T>) {
    println(pointer.rawValue)
}
