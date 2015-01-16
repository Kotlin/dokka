package org.jetbrains.dokka

import java.io.File

class YamlOutlineService(val locationService: LocationService,
                         val languageService: LanguageService) : OutlineFormatService {
    override fun getOutlineFileName(location: Location): File = File("${location.path}.yml")

    var outlineLevel = 0
    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        val indent = "    ".repeat(outlineLevel)
        to.appendln("$indent- title: ${languageService.renderName(node)}")
        to.appendln("$indent  url: ${locationService.location(node).path}")
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        val indent = "    ".repeat(outlineLevel)
        to.appendln("$indent  content:")
        outlineLevel++
        body()
        outlineLevel--
    }
}
