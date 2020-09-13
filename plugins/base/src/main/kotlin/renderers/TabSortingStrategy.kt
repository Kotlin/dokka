package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.Content

interface TabSortingStrategy {
    fun <T: Content> sort(tabs: Collection<T>) : List<T>
}