package org.jetbrains.dokka.javadoc

import javadoc.pages.rootIndexPage
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.Renderer

class JavadocRenderer(val outputWriter: OutputWriter, val context: DokkaContext) : Renderer {
    override fun render(root: RootPageNode) {
        outputWriter.writeHtml("index", (root as RendererSpecificPage).let {it.strategy as RenderingStrategy.Callback}.instructions(this, root)) // TODO get version
        root.children.forEach{renderPages(it, it.name)}
    }

    private fun renderPages(node: PageNode, dir: String = "") {
        val path = "$dir/${node.name}"
        if (node is RendererSpecificPage)
            when (val strategy = node.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, "")
                is RenderingStrategy.Write -> outputWriter.writeHtml(path, strategy.text)
                is RenderingStrategy.Callback -> outputWriter.writeHtml(path, strategy.instructions(this, node))
                RenderingStrategy.DoNothing -> outputWriter.writeHtml(path, "")
            }
        else
            outputWriter.writeHtml("$path/${node.name}", "")

        node.children.forEach { renderPages(it, path) }
    }

    private fun OutputWriter.writeHtml(path: String, text: String) = write(path, text, ".html")
}