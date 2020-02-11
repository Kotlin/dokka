package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.PageNode

interface PageMergerStrategy {

    fun tryMerge(pages: List<PageNode>): List<PageNode>

}