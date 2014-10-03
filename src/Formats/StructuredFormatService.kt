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
    public abstract fun formatSymbol(text: String): String
    public abstract fun formatKeyword(text: String): String
    public abstract fun formatIdentifier(text: String): String
    public abstract fun formatLink(text: String, location: Location): String
    public abstract fun formatLink(text: String, href: String): String
    public open fun formatLink(link: FormatLink): String = formatLink(formatText(link.text), link.location)
    public abstract fun formatBold(text: String): String
    public abstract fun formatCode(code: String): String
    public abstract fun formatBreadcrumbs(items: Iterable<FormatLink>): String

    open fun formatText(location: Location, nodes: Iterable<ContentNode>): String {
        return nodes.map { formatText(location, it) }.join("")
    }

    open fun formatText(location: Location, content: ContentNode): String {
        return StringBuilder {
            when (content) {
                is ContentText -> append(content.text)
                is ContentSymbol -> append(formatSymbol(content.text))
                is ContentKeyword -> append(formatKeyword(content.text))
                is ContentIdentifier -> append(formatIdentifier(content.text))
                is ContentEmphasis -> append(formatBold(formatText(location, content.children)))
                is ContentNodeLink -> {
                    val linkTo = locationService.relativeLocation(location, content.node, extension)
                    val linkText = formatText(location, content.children)
                    append(formatLink(linkText, linkTo))
                }
                is ContentExternalLink -> {
                    val linkText = formatText(location, content.children)
                    append(formatLink(linkText, content.href))
                }
                else -> append(formatText(location, content.children))
            }
        }.toString()
    }

    open public fun link(from: DocumentationNode, to: DocumentationNode): FormatLink = link(from, to, extension)

    open public fun link(from: DocumentationNode, to: DocumentationNode, extension: String): FormatLink {
        return FormatLink(to.name, locationService.relativeLocation(from, to, extension))
    }

    fun appendDescription(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val described = nodes.filter { !it.doc.isEmpty }
        if (described.any()) {
            val single = described.size == 1
            appendHeader(to, "Description", 3)
            for (node in described) {
                if (!single) {
                    appendBlockCode(to, formatText(location, languageService.render(node)))
                }
                appendLine(to, formatText(location,node.doc.description))
                appendLine(to)
                for ((label, section) in node.doc.sections) {
                    if (label.startsWith("$"))
                        continue
                    appendLine(to, formatBold(formatText(label)))
                    appendLine(to, formatText(location, section))
                    appendLine(to)
                }
            }
        }
    }

    fun appendSummary(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownBySummary = nodes.groupByTo(LinkedHashMap()) { node ->
            node.doc.summary
        }

        for ((summary, items) in breakdownBySummary) {
            items.forEach {
                appendBlockCode(to, formatText(location, languageService.render(it)))
            }
            appendLine(to, formatText(location, summary))
            appendLine(to)
        }
    }

    fun appendLocation(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownByName = nodes.groupBy { node -> node.name }
        for ((name, items) in breakdownByName) {
            appendHeader(to, formatText(name))
            appendSummary(location, to, items)
            appendDescription(location, to, items)
        }
    }

    private fun StructuredFormatService.appendSection(location : Location, caption: String, nodes: List<DocumentationNode>, node: DocumentationNode, to: StringBuilder) {
        if (nodes.any()) {
            appendHeader(to, caption, 3)

            val children = nodes.sortBy { it.name }
            val membersMap = children.groupBy { link(node, it) }

            appendTable(to) {
                appendTableBody(to) {
                    for ((memberLocation, members) in membersMap) {
                        appendTableRow(to) {
                            appendTableCell(to) {
                                appendText(to, formatLink(memberLocation))
                            }
                            appendTableCell(to) {
                                val breakdownBySummary = members.groupBy { it.doc.summary }
                                for ((summary, items) in breakdownBySummary) {
                                    for (signature in items) {
                                        appendBlockCode(to, formatText(location, languageService.render(signature)))
                                    }

                                    if (!summary.isEmpty()) {
                                        appendText(to, formatText(location, summary))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownByLocation = nodes.groupBy { node ->
            formatBreadcrumbs(node.path.map { link(node, it) })
        }

        for ((breadcrumbs, items) in breakdownByLocation) {
            appendLine(to, breadcrumbs)
            appendLine(to)
            appendLocation(location, to, items)
        }

        for (node in nodes) {
            appendSection(location, "Members", node.members, node, to)
            appendSection(location, "Extensions", node.extensions, node, to)
            appendSection(location, "Inheritors", node.inheritors, node, to)
            appendSection(location, "Links", node.links, node, to)

        }
    }

    abstract public fun appendOutlineHeader(to: StringBuilder, node: DocumentationNode)
    abstract public fun appendOutlineChildren(to: StringBuilder, nodes: Iterable<DocumentationNode>)

    public override fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            appendOutlineHeader(to, node)
            if (node.members.any()) {
                appendOutlineChildren(to, node.members)
            }
        }
    }
}