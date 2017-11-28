package org.jetbrains.dokka

import com.google.inject.Inject
import java.io.File

/**
 * Outline service for generating a _toc.yaml file, responsible for pointing to the paths of each
 * index.html file in the doc tree.
 */
class DacOutlineService @Inject constructor(val locationService: LocationService,
                                            val languageService: LanguageService) : OutlineFormatService {
    override fun getOutlineFile(location: Location): File = File("${location.path}.yaml")

    var outlineLevel = 0

    /** Appends formatted outline to [StringBuilder](to) using specified [location] */
    override fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        if (outlineLevel == 0) to.appendln("reference:")
        for (node in nodes) {
            appendOutlineHeader(location, node, to)
            val subPackages = node.members.filter {
                it.kind == NodeKind.Package
            }
            if (subPackages.any()) {
                val sortedMembers = subPackages.sortedBy { it.name }
                appendOutlineLevel(to) {
                    appendOutline(location, to, sortedMembers)
                }
            }

        }
    }

    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        if (node is DocumentationModule) {
            to.appendln("- title: Package Index")
            to.appendln("  path: /${locationService.location(node).path}.html")
            to.appendln("  status_text: no-toggle")
        } else {
            to.appendln("- title: ${languageService.renderName(node)}")
            to.appendln("  path: /${locationService.location(node).path}.html")
            to.appendln("  status_text: apilevel-")
        }
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        outlineLevel++
        body()
        outlineLevel--
    }
}
