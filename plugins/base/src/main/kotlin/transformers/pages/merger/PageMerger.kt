package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer

class PageMerger(context: DokkaContext) : PageTransformer {

    private val strategies: Iterable<PageMergerStrategy> = context.plugin<DokkaBase>().query { pageMergerStrategy }

    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(children = input.children.map { it.mergeChildren(emptyList()) })

    private fun PageNode.mergeChildren(path: List<String>): PageNode = children.groupBy { it::class }.map {
        it.value.groupBy { it.name }.map { (n, v) -> mergePageNodes(v, path + n) }.map { it.assertSingle(path) }
    }.let { pages ->
        modified(children = pages.flatten().map { it.mergeChildren(path + it.name) })
    }

    private fun mergePageNodes(pages: List<PageNode>, path: List<String>): List<PageNode> =
        strategies.fold(pages) { acc, strategy -> tryMerge(strategy, acc, path) }

    private fun tryMerge(strategy: PageMergerStrategy, pages: List<PageNode>, path: List<String>) =
        if (pages.size > 1) strategy.tryMerge(pages, path) else pages
}

private fun <T> Iterable<T>.assertSingle(path: List<String>): T = try {
        single()
    } catch (e: Exception) {
        val renderedPath = path.joinToString(separator = "/")
        throw IllegalStateException("Page merger is misconfigured. Error for $renderedPath: ${e.message}")
    }