package org.jetbrains.dokka.javadoc

import javadoc.pages.JavadocPageNode
import javadoc.pages.RootIndexPage
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.Renderer

class JavadocRenderer(val outputWriter: OutputWriter, val context: DokkaContext) : Renderer {
    override fun render(root: RootPageNode) {
        val rootIndex = (root as RootIndexPage)
        val instructions = (rootIndex.strategy as RenderingStrategy.Callback).instructions
        outputWriter.writeHtml("index", instructions(this, rootIndex)) // TODO get version
        rootIndex.children.forEach { renderPages(it) }
    }

    private fun renderPages(node: PageNode, dir: String = "") {
        val path = if (node is JavadocPageNode) node.fullPath else "$dir/${node.name}"
        if (node is RendererSpecificPage) {
            when (val strategy = node.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, "")
                is RenderingStrategy.Write -> outputWriter.writeHtml(path, strategy.text)
                is RenderingStrategy.Callback -> outputWriter.writeHtml(path, strategy.instructions(this, node))
                RenderingStrategy.DoNothing -> Unit
            }
        }
//        else
//            throw IllegalStateException("${node.name} was not expected here")

        node.children.forEach { renderPages(it, path) }
    }

    private fun OutputWriter.writeHtml(path: String, text: String) = write(path, text, ".html")
}