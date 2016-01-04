package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode
import java.util.*

data class FormatLink(val text: String, val href: String)

enum class ListKind {
    Ordered,
    Unordered
}

abstract class StructuredFormatService(locationService: LocationService,
                                              val languageService: LanguageService,
                                              override val extension: String,
                                              val linkExtension: String = extension) : FormatService {
    val locationService: LocationService = locationService.withExtension(linkExtension)

    abstract fun appendBlockCode(to: StringBuilder, line: String, language: String)
    abstract fun appendHeader(to: StringBuilder, text: String, level: Int = 1)
    abstract fun appendParagraph(to: StringBuilder, text: String)
    abstract fun appendLine(to: StringBuilder, text: String = "")
    abstract fun appendAnchor(to: StringBuilder, anchor: String)

    abstract fun appendTable(to: StringBuilder, body: () -> Unit)
    abstract fun appendTableHeader(to: StringBuilder, body: () -> Unit)
    abstract fun appendTableBody(to: StringBuilder, body: () -> Unit)
    abstract fun appendTableRow(to: StringBuilder, body: () -> Unit)
    abstract fun appendTableCell(to: StringBuilder, body: () -> Unit)

    abstract fun formatText(text: String): String
    abstract fun formatSymbol(text: String): String
    abstract fun formatKeyword(text: String): String
    abstract fun formatIdentifier(text: String, kind: IdentifierKind): String
    fun formatEntity(text: String): String = text
    abstract fun formatLink(text: String, href: String): String
    open fun formatLink(link: FormatLink): String = formatLink(formatText(link.text), link.href)
    abstract fun formatStrong(text: String): String
    abstract fun formatStrikethrough(text: String): String
    abstract fun formatEmphasis(text: String): String
    abstract fun formatCode(code: String): String
    abstract fun formatUnorderedList(text: String): String
    abstract fun formatOrderedList(text: String): String
    abstract fun formatListItem(text: String, kind: ListKind): String
    abstract fun formatBreadcrumbs(items: Iterable<FormatLink>): String
    abstract fun formatNonBreakingSpace(): String
    open fun formatSoftLineBreak(): String = ""
    open fun formatIndentedSoftLineBreak(): String = ""

    open fun formatText(location: Location, nodes: Iterable<ContentNode>, listKind: ListKind = ListKind.Unordered): String {
        return nodes.map { formatText(location, it, listKind) }.joinToString("")
    }

    open fun formatText(location: Location, content: ContentNode, listKind: ListKind = ListKind.Unordered): String {
        return StringBuilder().apply {
            when (content) {
                is ContentText -> append(formatText(content.text))
                is ContentSymbol -> append(formatSymbol(content.text))
                is ContentKeyword -> append(formatKeyword(content.text))
                is ContentIdentifier -> append(formatIdentifier(content.text, content.kind))
                is ContentNonBreakingSpace -> append(formatNonBreakingSpace())
                is ContentSoftLineBreak -> append(formatSoftLineBreak())
                is ContentIndentedSoftLineBreak -> append(formatIndentedSoftLineBreak())
                is ContentEntity -> append(formatEntity(content.text))
                is ContentStrong -> append(formatStrong(formatText(location, content.children)))
                is ContentStrikethrough -> append(formatStrikethrough(formatText(location, content.children)))
                is ContentCode -> append(formatCode(formatText(location, content.children)))
                is ContentEmphasis -> append(formatEmphasis(formatText(location, content.children)))
                is ContentUnorderedList -> append(formatUnorderedList(formatText(location, content.children, ListKind.Unordered)))
                is ContentOrderedList -> append(formatOrderedList(formatText(location, content.children, ListKind.Ordered)))
                is ContentListItem -> append(formatListItem(formatText(location, content.children), listKind))

                is ContentNodeLink -> {
                    val node = content.node
                    val linkTo = if (node != null) locationHref(location, node) else "#"
                    val linkText = formatText(location, content.children)
                    if (linkTo == ".") {
                        append(linkText)
                    } else {
                        append(formatLink(linkText, linkTo))
                    }
                }
                is ContentExternalLink -> {
                    val linkText = formatText(location, content.children)
                    if (content.href == ".") {
                        append(linkText)
                    } else {
                        append(formatLink(linkText, content.href))
                    }
                }
                is ContentParagraph -> appendParagraph(this, formatText(location, content.children))
                is ContentBlockCode -> appendBlockCode(this, formatText(location, content.children), content.language)
                is ContentHeading -> appendHeader(this, formatText(location, content.children), content.level)
                is ContentBlock -> append(formatText(location, content.children))
            }
        }.toString()
    }

    open fun link(from: DocumentationNode, to: DocumentationNode): FormatLink = link(from, to, extension)

    open fun link(from: DocumentationNode, to: DocumentationNode, extension: String): FormatLink {
        return FormatLink(to.name, locationService.relativePathToLocation(from, to))
    }

    fun locationHref(from: Location, to: DocumentationNode): String {
        val topLevelPage = to.references(RefKind.TopLevelPage).singleOrNull()?.to
        if (topLevelPage != null) {
            return from.relativePathTo(locationService.location(topLevelPage), to.name)
        }
        return from.relativePathTo(locationService.location(to))
    }

    fun appendDocumentation(location: Location, to: StringBuilder, overloads: Iterable<DocumentationNode>) {
        val breakdownBySummary = overloads.groupByTo(LinkedHashMap()) { node -> node.content }

        if (breakdownBySummary.size == 1) {
            formatOverloadGroup(breakdownBySummary.values.single(), location, to)
        }
        else {
            for ((summary, items) in breakdownBySummary) {
                appendAsOverloadGroup(to) {
                    formatOverloadGroup(items, location, to)
                }
            }
        }
    }

    private fun formatOverloadGroup(items: MutableList<DocumentationNode>, location: Location, to: StringBuilder) {
        items.forEach {
            val rendered = languageService.render(it)
            appendAsSignature(to, rendered) {
                to.append(formatCode(formatText(location, rendered)))
                it.appendSourceLink(to)
            }
            it.appendOverrides(to)
            it.appendDeprecation(location, to)
        }
        // All items have exactly the same documentation, so we can use any item to render it
        val item = items.first()
        item.details(NodeKind.OverloadGroupNote).forEach {
            to.append(formatText(location, it.content))
        }
        to.append(formatText(location, item.content.summary))
        appendDescription(location, to, item)
        appendLine(to)
        appendLine(to)
    }

    private fun DocumentationNode.isModuleOrPackage(): Boolean =
        kind == NodeKind.Module || kind == NodeKind.Package

    protected open fun appendAsSignature(to: StringBuilder, node: ContentNode, block: () -> Unit) {
        block()
    }

    protected open fun appendAsOverloadGroup(to: StringBuilder, block: () -> Unit) {
        block()
    }

    fun appendDescription(location: Location, to: StringBuilder, node: DocumentationNode) {
        if (node.content.description != ContentEmpty) {
            appendLine(to, formatText(location, node.content.description))
            appendLine(to)
        }
        node.content.getSectionsWithSubjects().forEach {
            appendSectionWithSubject(it.key, location, it.value, to)
        }

        for (section in node.content.sections.filter { it.subjectName == null }) {
            appendLine(to, formatStrong(formatText(section.tag)))
            appendLine(to, formatText(location, section))
        }
    }

    fun Content.getSectionsWithSubjects(): Map<String, List<ContentSection>> =
            sections.filter { it.subjectName != null }.groupBy { it.tag }

    fun appendSectionWithSubject(title: String, location: Location, subjectSections: List<ContentSection>, to: StringBuilder) {
        appendHeader(to, title, 3)
        subjectSections.forEach {
            val subjectName = it.subjectName
            if (subjectName != null) {
                appendAnchor(to, subjectName)
                to.append(formatCode(subjectName)).append(" - ")
                to.append(formatText(location, it))
                appendLine(to)
            }
        }
    }

    private fun DocumentationNode.appendOverrides(to: StringBuilder) {
        overrides.forEach {
            to.append("Overrides ")
            val location = locationService.relativePathToLocation(this, it)
            appendLine(to, formatLink(FormatLink(it.owner!!.name + "." + it.name, location)))
        }
    }

    private fun DocumentationNode.appendDeprecation(location: Location, to: StringBuilder) {
        if (deprecation != null) {
            val deprecationParameter = deprecation!!.details(NodeKind.Parameter).firstOrNull()
            val deprecationValue = deprecationParameter?.details(NodeKind.Value)?.firstOrNull()
            if (deprecationValue != null) {
                to.append(formatStrong("Deprecated:")).append(" ")
                appendLine(to, formatText(deprecationValue.name.removeSurrounding("\"")))
                appendLine(to)
            } else if (deprecation?.content != Content.Empty) {
                to.append(formatStrong("Deprecated:")).append(" ")
                to.append(formatText(location, deprecation!!.content))
            } else {
                appendLine(to, formatStrong("Deprecated"))
                appendLine(to)
            }
        }
    }

    private fun DocumentationNode.appendSourceLink(to: StringBuilder) {
        val sourceUrl = details(NodeKind.SourceUrl).firstOrNull()
        if (sourceUrl != null) {
            to.append(" ")
            appendLine(to, formatLink("(source)", sourceUrl.name))
        } else {
            appendLine(to)
        }
    }

    fun appendLocation(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val singleNode = nodes.singleOrNull()
        if (singleNode != null && singleNode.isModuleOrPackage()) {
            if (singleNode.kind == NodeKind.Package) {
                appendHeader(to, "Package " + formatText(singleNode.name), 2)
            }
            to.append(formatText(location, singleNode.content))
        } else {
            val breakdownByName = nodes.groupBy { node -> node.name }
            for ((name, items) in breakdownByName) {
                appendHeader(to, formatText(name))
                appendDocumentation(location, to, items)
            }
        }
    }

    private fun appendSection(location: Location, caption: String, nodes: List<DocumentationNode>, node: DocumentationNode, to: StringBuilder) {
        if (nodes.any()) {
            appendHeader(to, caption, 3)

            val children = nodes.sortedBy { it.name }
            val membersMap = children.groupBy { link(node, it) }

            appendTable(to) {
                appendTableBody(to) {
                    for ((memberLocation, members) in membersMap) {
                        appendTableRow(to) {
                            appendTableCell(to) {
                                to.append(formatLink(memberLocation))
                            }
                            appendTableCell(to) {
                                val breakdownBySummary = members.groupBy { formatText(location, it.summary) }
                                for ((summary, items) in breakdownBySummary) {
                                    appendSummarySignatures(items, location, to)
                                    if (!summary.isEmpty()) {
                                        to.append(summary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun appendSummarySignatures(items: List<DocumentationNode>, location: Location, to: StringBuilder) {
        val summarySignature = languageService.summarizeSignatures(items)
        if (summarySignature != null) {
            appendAsSignature(to, summarySignature) {
                appendLine(to, summarySignature.signatureToText(location))
            }
            return
        }
        val renderedSignatures = items.map { languageService.render(it, RenderMode.SUMMARY) }
        renderedSignatures.subList(0, renderedSignatures.size - 1).forEach {
            appendAsSignature(to, it) {
                appendLine(to, it.signatureToText(location))
            }
        }
        appendAsSignature(to, renderedSignatures.last()) {
            to.append(renderedSignatures.last().signatureToText(location))
        }
    }

    private fun ContentNode.signatureToText(location: Location): String {
        return if (this is ContentBlock && this.isEmpty()) {
            ""
        } else {
            val signatureAsCode = ContentCode()
            signatureAsCode.append(this)
            formatText(location, signatureAsCode)
        }
    }

    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val breakdownByLocation = nodes.groupBy { node ->
            formatBreadcrumbs(node.path.filterNot { it.name.isEmpty() }.map { link(node, it) })
        }

        for ((breadcrumbs, items) in breakdownByLocation) {
            appendLine(to, breadcrumbs)
            appendLine(to)
            appendLocation(location, to, items.filter { it.kind != NodeKind.ExternalClass })
        }

        for (node in nodes) {
            if (node.kind == NodeKind.ExternalClass) {
                appendSection(location, "Extensions for ${node.name}", node.members, node, to)
                continue
            }

            appendSection(location, "Packages", node.members(NodeKind.Package), node, to)
            appendSection(location, "Types", node.members.filter { it.kind in NodeKind.classLike }, node, to)
            appendSection(location, "Extensions for External Classes", node.members(NodeKind.ExternalClass), node, to)
            appendSection(location, "Enum Values", node.members(NodeKind.EnumItem), node, to)
            appendSection(location, "Constructors", node.members(NodeKind.Constructor), node, to)
            appendSection(location, "Properties", node.members(NodeKind.Property), node, to)
            appendSection(location, "Inherited Properties", node.inheritedMembers(NodeKind.Property), node, to)
            appendSection(location, "Functions", node.members(NodeKind.Function), node, to)
            appendSection(location, "Inherited Functions", node.inheritedMembers(NodeKind.Function), node, to)
            appendSection(location, "Companion Object Properties", node.members(NodeKind.CompanionObjectProperty), node, to)
            appendSection(location, "Companion Object Functions", node.members(NodeKind.CompanionObjectFunction), node, to)
            appendSection(location, "Other members", node.members.filter {
                it.kind !in setOf(
                        NodeKind.Class,
                        NodeKind.Interface,
                        NodeKind.Enum,
                        NodeKind.Object,
                        NodeKind.AnnotationClass,
                        NodeKind.Constructor,
                        NodeKind.Property,
                        NodeKind.Package,
                        NodeKind.Function,
                        NodeKind.CompanionObjectProperty,
                        NodeKind.CompanionObjectFunction,
                        NodeKind.ExternalClass,
                        NodeKind.EnumItem
                        )
            }, node, to)

            val allExtensions = collectAllExtensions(node)
            appendSection(location, "Extension Properties", allExtensions.filter { it.kind == NodeKind.Property }, node, to)
            appendSection(location, "Extension Functions", allExtensions.filter { it.kind == NodeKind.Function }, node, to)
            appendSection(location, "Companion Object Extension Properties", allExtensions.filter { it.kind == NodeKind.CompanionObjectProperty }, node, to)
            appendSection(location, "Companion Object Extension Functions", allExtensions.filter { it.kind == NodeKind.CompanionObjectFunction }, node, to)
            appendSection(location, "Inheritors",
                    node.inheritors.filter { it.kind != NodeKind.EnumItem }, node, to)
            appendSection(location, "Links", node.links, node, to)

        }
    }

    private fun collectAllExtensions(node: DocumentationNode): Collection<DocumentationNode> {
        val result = LinkedHashSet<DocumentationNode>()
        val visited = hashSetOf<DocumentationNode>()

        fun collect(node: DocumentationNode) {
            if (!visited.add(node)) return
            result.addAll(node.extensions)
            node.references(RefKind.Superclass).forEach { collect(it.to) }
        }

        collect(node)

        return result

    }
}