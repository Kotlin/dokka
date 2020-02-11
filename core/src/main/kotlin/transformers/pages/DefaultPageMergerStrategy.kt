package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.utilities.DokkaLogger

class DefaultPageMergerStrategy(val logger: DokkaLogger) : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>): List<PageNode> {
        if (pages.size != 1) logger.warn("Expected 1 page, but got ${pages.size}")
        return listOf(pages.first())
    }
}