package org.jetbrains.dokka

public class HtmlFormatService(val locationService: LocationService,
                               val signatureGenerator: SignatureGenerator) : FormatService {
    override val extension: String = "html"
    override fun format(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        for (node in nodes) {
            with (to) {
                appendln("<h2>")
                appendln("Summary for ${node.name}")
                appendln("</h2>")
                appendln("<code>")
                appendln(signatureGenerator.render(node))
                appendln("</code>")
                appendln()
                appendln("<p>")
                appendln(node.doc.summary)
                appendln("</p>")
                appendln("<hr/>")

                for (section in node.doc.sections) {
                    appendln("<h3>")
                    appendln(section.label)
                    appendln("</h3>")
                    appendln("<p>")
                    appendln(section.text)
                    appendln("</p>")
                }

                appendln("<h3>")
                appendln("Members")
                appendln("</h3>")
                appendln("<table>")

                appendln("<thead>")
                appendln("<tr>")
                appendln("<td>Member</td>")
                appendln("<td>Signature</td>")
                appendln("<td>Summary</td>")
                appendln("</tr>")
                appendln("</thead>")

                appendln("<tbody>")
                for (member in node.members.sortBy { it.name }) {
                    val relativePath = locationService.relativeLocation(node, member, extension)
                    appendln("<tr>")
                    appendln("<td>")
                    append("<a href=\"${relativePath}\">${member.name}</a>")
                    appendln("</td>")
                    appendln("<td>")
                    append("${signatureGenerator.render(member)}")
                    appendln("</td>")
                    appendln("<td>")
                    append("${member.doc.summary}")
                    appendln("</td>")
                    appendln("</tr>")
                }
                appendln("</tbody>")
                appendln("</table>")

            }
        }
    }
}