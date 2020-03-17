package org.jetbrains.dokka.javadoc

import javadoc.pages.rootIndexPage
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.OutputWriter
import org.jetbrains.dokka.renderers.Renderer

class JavadocRenderer(val outputWriter: OutputWriter, val context: DokkaContext) : Renderer {
    override fun render(root: RootPageNode) {
        outputWriter.write("index", rootIndexPage(root.name, "0.0.1")) // TODO get version

    }

    private fun OutputWriter.write(path: String, text: String) = write(path, text, ".html")
}