package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "SelfRepresentingSingletonSet is an incorrect set implementation that breaks set invariants", level = DeprecationLevel.ERROR)
interface SelfRepresentingSingletonSet<T : SelfRepresentingSingletonSet<T>> : Set<T> {
    override val size: Int get() = 1

    override fun contains(element: T): Boolean = this == element

    override fun containsAll(elements: Collection<T>): Boolean =
        if (elements.isEmpty()) true
        else elements.all { this == it }

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<T> = iterator {
        @Suppress("UNCHECKED_CAST")
        yield(this@SelfRepresentingSingletonSet as T)
    }
}
