package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.utilities.DokkaLogger

class FallbackPageMergerStrategy(private val logger: DokkaLogger) : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode> {
        val renderedPath = path.joinToString(separator = "/")
        if (pages.size != 1) logger.warn("For $renderedPath: expected 1 page, but got ${pages.size}")
        return listOf(pages.first())
    }
}