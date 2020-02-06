package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.PageNode

abstract class PageMergerStrategy {

    abstract fun tryMerge(pages: List<PageNode>): List<PageNode>

}