package org.jetbrains.dokka.html.tabs

import org.jetbrains.dokka.pages.ContentNode

interface TabSortingStrategy {
    fun <T: ContentNode> sort(tabs: Collection<T>) : List<T>
}