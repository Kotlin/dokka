package org.jetbrains.dokka.kotlinAsJava

inline fun <T> MutableList<T>.removeLastIf(predicate: (T) -> Boolean): Boolean {
    val lastIndex = indexOfLast(predicate)
    return if (lastIndex < 0) {
        false
    } else {
        removeAt(lastIndex)
        true
    }
}