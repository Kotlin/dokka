package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import org.jetbrains.dokka.*
import java.net.URI

/**
 * Outline service for generating a _toc.yaml file, responsible for pointing to the paths of each
 * index.html file in the doc tree.
 */
class DacOutlineService(
        val uriProvider: JavaLayoutHtmlUriProvider,
        val languageService: LanguageService
) : DacOutlineFormatService {
    override fun computeOutlineURI(node: DocumentationNode): URI = uriProvider.containerUri(node).resolve("_toc.yaml")

    override fun format(uri: URI, to: Appendable, node: DocumentationNode) {
        appendOutline(uri, to, listOf(node))
    }

    var outlineLevel = 0

    /** Appends formatted outline to [StringBuilder](to) using specified [location] */
    fun appendOutline(uri: URI, to: Appendable, nodes: Iterable<DocumentationNode>) {
        if (outlineLevel == 0) to.appendln("reference:")
        for (node in nodes) {
            appendOutlineHeader(uri, node, to)
            val subPackages = node.members.filter {
                it.kind == NodeKind.Package
            }
            if (subPackages.any()) {
                val sortedMembers = subPackages.sortedBy { it.name }
                appendOutlineLevel(to) {
                    appendOutline(uri, to, sortedMembers)
                }
            }

        }
    }

    fun appendOutlineHeader(uri: URI, node: DocumentationNode, to: Appendable) {
        if (node is DocumentationModule) {
            to.appendln("- title: Package Index")
            to.appendln("  path: ${uriProvider.mainUriOrWarn(node)}")
            to.appendln("  status_text: no-toggle")
        } else {
            to.appendln("- title: ${languageService.renderName(node)}")
            to.appendln("  path: ${uriProvider.mainUriOrWarn(node)}")
            to.appendln("  status_text: apilevel-")
        }
    }

    fun appendOutlineLevel(to: Appendable, body: () -> Unit) {
        outlineLevel++
        body()
        outlineLevel--
    }
}


interface DacOutlineFormatService {
    fun computeOutlineURI(node: DocumentationNode): URI
    fun format(uri: URI, to: Appendable, node: DocumentationNode)
}

class DacOutlineFormatter @Inject constructor(
        uriProvider: JavaLayoutHtmlUriProvider,
        languageService: LanguageService
) : JavaLayoutHtmlFormatOutlineFactoryService {
    val baseOutline = DacOutlineService(uriProvider, languageService)
    val navOutline = DacNavOutlineService(uriProvider, languageService)
    val searchOutline = DacSearchOutlineService(uriProvider, languageService)

    val outlines = listOf(baseOutline, navOutline, searchOutline)

    override fun generateOutlines(outputProvider: (URI) -> Appendable, nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            for (outline in outlines) {
                val uri = outline.computeOutlineURI(node)
                val output = outputProvider(uri)
                outline.format(uri, output, node)
            }
        }
    }
}