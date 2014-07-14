package org.jetbrains.dokka

public class MarkdownFormatService(val locationService: LocationService,
                                   val signatureGenerator: SignatureGenerator) : FormatService {
    override val extension: String = "md"
    override fun format(node: DocumentationNode, to: StringBuilder) {
        with (to) {
            appendln(node.path.map { "[${it.name}](${locationService.relativeLocation(node, it, extension)})" }.joinToString(" / "))
            appendln()
            append("# ")
            appendln(signatureGenerator.render(node))
            appendln()
            appendln(node.doc.summary)
            appendln()
            for (section in node.doc.sections) {
                append("### ")
                appendln(section.label)
                appendln(section.text)
            }


            appendln("### Members")
            appendln("| Name | Signature | Summary |")
            appendln("|------|-----------|---------|")
            for (member in node.members.sortBy { it.name }) {
                val relativePath = locationService.relativeLocation(node, member, extension)
                append("|[${member.name}](${relativePath})")
                append("|${signatureGenerator.render(member)}")
                append("|${member.doc.summary} ")
                appendln("|")
            }
        }
    }
}