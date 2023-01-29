package org.jetbrains.dokka.utilities

internal inline fun <reified T> Any.cast(): T = this as T
