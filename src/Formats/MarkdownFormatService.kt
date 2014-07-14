package org.jetbrains.dokka

import org.jetbrains.dokka.DocumentationNode.Kind

public class MarkdownFormatService(val locationService: LocationService,
                                   val signatureGenerator: SignatureGenerator) : FormatService {
    override val extension: String = "md"
    override fun format(node: DocumentationNode, to: StringBuilder) {
        with (to) {
            appendln(node.path.map { "[${it.name}](${locationService.relativeLocation(node, it, extension)})" }.joinToString(" / "))
            appendln()
            appendln("# ${node.name}")
            appendln(node.doc.summary)
            appendln("```")
            appendln(signatureGenerator.render(node))
            appendln("```")
            appendln(node.doc.description)
            appendln()
            for (section in node.doc.sections) {
                append("##### ")
                append(section.label)
                appendln()
                append(section.text)
                appendln()
            }

            if (node.members.any()) {
                appendln("### Members")
                appendln("| Name | Signature | Summary |")
                appendln("|------|-----------|---------|")
                for (member in node.members.sortBy { it.name }) {
                    val relativePath = locationService.relativeLocation(node, member, extension)
                    val displayName = when (member.kind) {
                        Kind.Constructor -> "*.init*"
                        else -> signatureGenerator.renderName(member).htmlEscape()
                    }
                    append("|[${displayName}](${relativePath})")
                    append("|`${signatureGenerator.render(member)}`")
                    append("|${member.doc.summary} ")
                    appendln("|")
                }
            }
        }
    }
}
