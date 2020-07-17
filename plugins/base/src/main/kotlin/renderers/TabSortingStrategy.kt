package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.ContentNode

interface TabSortingStrategy {
    fun <T: ContentNode> sort(tabs: Collection<T>) : List<T>
}