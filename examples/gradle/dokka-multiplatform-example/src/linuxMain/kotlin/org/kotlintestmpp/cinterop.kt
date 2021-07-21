@file:Suppress("unused")

package org.kotlintestmpp

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer

fun<T: CPointed> printPointerRawValue(pointer: CPointer<T>) {
    println(pointer.rawValue)
}
