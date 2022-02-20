package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.pages.PageNode

fun interface PageMergerStrategy {

    fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode>

}