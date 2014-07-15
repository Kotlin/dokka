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

    public abstract fun formatLink(text: String, location: Location): String
    public open fun formatLink(link: FormatLink): String = formatLink(link.text, link.location)

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
                appendLine(to, node.doc.description)
                appendLine(to)
                for (section in node.doc.sections) {
                    appendLine(to, formatBold(section.label))
                    appendLine(to, section.text)
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
        val breakdownByName = nodes.groupByTo(LinkedHashMap()) { node -> node.name }
        for ((name, items) in breakdownByName) {
            appendHeader(to, "${name}")
            appendSummary(to, items)
            appendDescription(to, items)
        }
    }

    override fun appendNodes(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownByLocation = nodes.groupByTo(LinkedHashMap()) { node ->
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

                appendLine(to, "| Name | Summary |") // TODO: hardcoded
                appendLine(to, "|------|---------|")
                val children = node.members.sortBy { it.name }
                val membersMap = children.groupByTo(LinkedHashMap()) { link(node, it) }

                for ((location, members) in membersMap) {
                    appendText(to, "|${formatLink(location)}|")
                    val breakdownBySummary = members.groupByTo(LinkedHashMap()) { it.doc.summary }
                    for ((summary, items) in breakdownBySummary) {
                        if (!summary.isEmpty()) {
                            appendText(to, summary)
                            to.append("<br/>") // TODO: hardcoded
                        }

                        val signatures = items.map { formatBold(formatCode("${languageService.render(it)}")) }
                        to.append(signatures.join("<br/>")) // TODO: hardcoded
                    }

                    appendLine(to, "|")
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
    public abstract fun formatText(text: String): String
}