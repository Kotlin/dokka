package org.jetbrains.dokka

import java.util.LinkedHashMap

public data class FormatLink(val text: String, val location: Location)

public abstract class StructuredFormatService(val locationService: LocationService,
                                              val languageService: LanguageService) : FormatService {

    abstract public fun appendBlockCode(to: StringBuilder, line: String)
    abstract public fun appendBlockCode(to: StringBuilder, lines: Iterable<String>)
    abstract public fun appendHeader(to: StringBuilder, text: String, level: Int = 1)
    abstract public fun appendText(to: StringBuilder, text: String)
    abstract public fun appendLine(to: StringBuilder, text: String)
    public abstract fun appendLine(to: StringBuilder)

    public abstract fun appendTable(to: StringBuilder, body: () -> Unit)
    public abstract fun appendTableHeader(to: StringBuilder, body: () -> Unit)
    public abstract fun appendTableBody(to: StringBuilder, body: () -> Unit)
    public abstract fun appendTableRow(to: StringBuilder, body: () -> Unit)
    public abstract fun appendTableCell(to: StringBuilder, body: () -> Unit)

    public abstract fun formatText(text: String): String
    public abstract fun formatLink(text: String, location: Location): String
    public open fun formatLink(link: FormatLink): String = formatLink(formatText(link.text), link.location)
    public abstract fun formatBold(text: String): String
    public abstract fun formatCode(code: String): String
    public abstract fun formatBreadcrumbs(items: Iterable<FormatLink>): String

    open public fun link(from: DocumentationNode, to: DocumentationNode): FormatLink = link(from, to, extension)

    open public fun link(from: DocumentationNode, to: DocumentationNode, extension: String): FormatLink {
        return FormatLink(to.name, locationService.relativeLocation(from, to, extension))
    }

    open public fun appendDescription(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val described = nodes.filter { it.doc.hasDescription }
        if (described.any()) {
            val single = described.size == 1
            appendHeader(to, "Description", 3)
            for (node in described) {
                if (!single) {
                    appendBlockCode(to, languageService.render(node))
                }
                appendLine(to, formatText(node.doc.description))
                appendLine(to)
                for (section in node.doc.sections) {
                    appendLine(to, formatBold(formatText(section.label)))
                    appendLine(to, formatText(section.text))
                    appendLine(to)
                }
            }
        }
    }

    open public fun appendSummary(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownBySummary = nodes.groupByTo(LinkedHashMap()) { node ->
            node.doc.summary
        }

        for ((summary, items) in breakdownBySummary) {
            appendLine(to, summary)
            appendBlockCode(to, items.map { languageService.render(it) })
        }
    }

    open public fun appendLocation(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownByName = nodes.groupBy { node -> node.name }
        for ((name, items) in breakdownByName) {
            appendHeader(to, formatText(name))
            appendSummary(to, items)
            appendDescription(to, items)
        }
    }

    override fun appendNodes(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownByLocation = nodes.groupBy { node ->
            formatBreadcrumbs(node.path.map { link(node, it) })
        }

        for ((breadcrumbs, items) in breakdownByLocation) {
            appendLine(to, breadcrumbs)
            appendLine(to)
            appendLocation(to, items)
        }

        for (node in nodes) {
            if (node.members.any()) {
                appendHeader(to, "Members", 3)

                val children = node.members.sortBy { it.name }
                val membersMap = children.groupBy { link(node, it) }

                appendTable(to) {
                    appendTableBody(to) {
                        for ((location, members) in membersMap) {
                            appendTableRow(to) {
                                appendTableCell(to) {
                                    appendText(to, formatLink(location))
                                }
                                appendTableCell(to) {
                                    val breakdownBySummary = members.groupBy { it.doc.summary }
                                    for ((summary, items) in breakdownBySummary) {
                                        if (!summary.isEmpty()) {
                                            appendText(to, formatText(summary))
                                            to.append("<br/>") // TODO: hardcoded
                                        }

                                        val signatures = items.map { formatBold(formatCode("${languageService.render(it)}")) }
                                        to.append(signatures.join("<br/>")) // TODO: hardcoded
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    abstract public fun appendOutlineHeader(to: StringBuilder, node: DocumentationNode)
    abstract public fun appendOutlineChildren(to: StringBuilder, nodes: Iterable<DocumentationNode>)

    override public fun appendOutline(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            appendOutlineHeader(to, node)
            if (node.members.any()) {
                appendOutlineChildren(to, node.members)
            }
        }
    }
}