package org.jetbrains.dokka

import com.google.inject.Inject
import java.io.File

class YamlOutlineService @Inject constructor(
        val generator: NodeLocationAwareGenerator,
        val languageService: LanguageService
) : OutlineFormatService {
    override fun getOutlineFileName(location: Location): File = File("${location.path}.yml")

    var outlineLevel = 0
    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        val indent = "    ".repeat(outlineLevel)
        to.appendln("$indent- title: ${languageService.renderName(node)}")
        to.appendln("$indent  url: ${generator.location(node).path}")
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        val indent = "    ".repeat(outlineLevel)
        to.appendln("$indent  content:")
        outlineLevel++
        body()
        outlineLevel--
    }
}
