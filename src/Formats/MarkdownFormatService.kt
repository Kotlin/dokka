package org.jetbrains.dokka

import org.jetbrains.dokka.DocumentationNode.Kind
import java.util.LinkedHashMap

public class MarkdownFormatService(val locationService: LocationService,
                                   val signatureGenerator: SignatureGenerator) : FormatService {
    override val extension: String = "md"
    override fun format(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        with (to) {
            val breakdown = nodes.groupByTo(LinkedHashMap()) { node ->
                node.path.map { "[${it.name}](${locationService.relativeLocation(node, it, extension)})" }.joinToString(" / ")
            }
            for ((path, items) in breakdown) {
                appendln(path)
                appendln()
                formatLocation(items)
            }

            for (node in nodes) {
                if (node.members.any()) {
                    appendln("## Members")
                    appendln("| Name | Summary |")
                    appendln("|------|---------|")
                    val children = node.members.sortBy { it.name }
                    val membersMap = children.groupByTo(LinkedHashMap()) { locationService.relativeLocation(node, it, extension) }
                    for ((location, members) in membersMap) {
                        val mainMember = members.first()
                        val displayName = when (mainMember.kind) {
                            Kind.Constructor -> "*.init*"
                            else -> signatureGenerator.renderName(mainMember).htmlEscape()
                        }
                        append("|[${displayName}](${location})|")
                        append(members.groupByTo(LinkedHashMap()) { it.doc.summary }.map { group ->
                            val (summary, items) = group
                            StringBuilder {
                                if (!summary.isEmpty()) {
                                    append("${summary}<br>")
                                }
                                for (item in items) {
                                    append("&nbsp;&nbsp;`${signatureGenerator.render(item)}`<br>")
                                }
                            }.toString()
                        }.joinToString("<br>"))
                        appendln("|")
                    }
                }

            }
        }
    }


    private fun StringBuilder.formatLocation(nodes: Iterable<DocumentationNode>) {
        val breakdown = nodes.groupByTo(LinkedHashMap()) { node ->
            node.name
        }
        for ((name, items) in breakdown) {
            appendln("# ${name}")
            formatSummary(items)
        }
    }

    private fun StringBuilder.formatSummary(nodes: Iterable<DocumentationNode>) {
        val breakdown = nodes.groupByTo(LinkedHashMap()) { node ->
            node.doc.summary
        }
        for ((summary, items) in breakdown) {
            appendln(summary)
            appendln("```")
            for (item in items)
                appendln(signatureGenerator.render(item))
            appendln("```")
        }

        val described = nodes.filter { it.doc.hasDescription }
        if (described.any()) {
            appendln("## Description")
            for (node in described) {
                appendln("```")
                appendln(signatureGenerator.render(node))
                appendln("```")
                appendln(node.doc.description)
                appendln()
                for (section in node.doc.sections) {
                    append("**")
                    append(section.label)
                    append("**")
                    appendln()
                    append(section.text)
                    appendln()
                    appendln()
                }
            }
        }
    }

}
