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

    private fun DocumentationNode.isModuleOrPackage(): Boolean =
            kind == NodeKind.Module || kind == NodeKind.Package

    protected open fun appendAsSignature(to: StringBuilder, node: ContentNode, block: () -> Unit) {
        block()
    }

    protected open fun appendAsOverloadGroup(to: StringBuilder, block: () -> Unit) {
        block()
    }

    fun Content.getSectionsWithSubjects(): Map<String, List<ContentSection>> =
            sections.filter { it.subjectName != null }.groupBy { it.tag }

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

    private fun ContentNode.signatureToText(location: Location): String {
        return if (this is ContentBlock && this.isEmpty()) {
            ""
        } else {
            val signatureAsCode = ContentCode()
            signatureAsCode.append(this)
            formatText(location, signatureAsCode)
        }
    }

    open inner class PageBuilder(val location: Location, val to: StringBuilder, val nodes: Iterable<DocumentationNode>) {
        open fun build() {
            val breakdownByLocation = nodes.groupBy { node ->
                formatBreadcrumbs(node.path.filterNot { it.name.isEmpty() }.map { link(node, it) })
            }

            for ((breadcrumbs, items) in breakdownByLocation) {
                appendLine(to, breadcrumbs)
                appendLine(to)
                appendLocation(items.filter { it.kind != NodeKind.ExternalClass })
            }
        }

        private fun appendLocation(nodes: Iterable<DocumentationNode>) {
            val singleNode = nodes.singleOrNull()
            if (singleNode != null && singleNode.isModuleOrPackage()) {
                if (singleNode.kind == NodeKind.Package) {
                    val packageName = if (singleNode.name.isEmpty()) "<root>" else singleNode.name
                    appendHeader(to, "Package " + formatText(packageName), 2)
                }
                to.append(formatText(location, singleNode.content))
            } else {
                val breakdownByName = nodes.groupBy { node -> node.name }
                for ((name, items) in breakdownByName) {
                    appendHeader(to, formatText(name))
                    appendDocumentation(items)
                }
            }
        }

        private fun appendDocumentation(overloads: Iterable<DocumentationNode>) {
            val breakdownBySummary = overloads.groupByTo(LinkedHashMap()) { node -> node.content }

            if (breakdownBySummary.size == 1) {
                formatOverloadGroup(breakdownBySummary.values.single())
            } else {
                for ((summary, items) in breakdownBySummary) {
                    appendAsOverloadGroup(to) {
                        formatOverloadGroup(items)
                    }
                }
            }
        }

        private fun formatOverloadGroup(items: MutableList<DocumentationNode>) {
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
            appendDescription(item)
            appendLine(to)
            appendLine(to)
        }

        private fun appendDescription(node: DocumentationNode) {
            if (node.content.description != ContentEmpty) {
                appendLine(to, formatText(location, node.content.description))
                appendLine(to)
            }
            node.content.getSectionsWithSubjects().forEach {
                appendSectionWithSubject(it.key, it.value)
            }

            for (section in node.content.sections.filter { it.subjectName == null }) {
                appendLine(to, formatStrong(formatText(section.tag)))
                appendLine(to, formatText(location, section))
            }
        }

        fun appendSectionWithSubject(title: String, subjectSections: List<ContentSection>) {
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
    }

    inner class SingleNodePageBuilder(location: Location, to: StringBuilder, val node: DocumentationNode)
        : PageBuilder(location, to, listOf(node)) {

        override fun build() {
            super.build()

            if (node.kind == NodeKind.ExternalClass) {
                appendSection("Extensions for ${node.name}", node.members)
                return
            }

            appendSection("Packages", node.members(NodeKind.Package))
            appendSection("Types", node.members.filter { it.kind in NodeKind.classLike && it.kind != NodeKind.AnnotationClass && it.kind != NodeKind.Exception })
            appendSection("Annotations", node.members(NodeKind.AnnotationClass))
            appendSection("Exceptions", node.members(NodeKind.Exception))
            appendSection("Extensions for External Classes", node.members(NodeKind.ExternalClass))
            appendSection("Enum Values", node.members(NodeKind.EnumItem))
            appendSection("Constructors", node.members(NodeKind.Constructor))
            appendSection("Properties", node.members(NodeKind.Property))
            appendSection("Inherited Properties",  node.inheritedMembers(NodeKind.Property))
            appendSection("Functions", node.members(NodeKind.Function))
            appendSection("Inherited Functions", node.inheritedMembers(NodeKind.Function))
            appendSection("Companion Object Properties", node.members(NodeKind.CompanionObjectProperty))
            appendSection("Companion Object Functions", node.members(NodeKind.CompanionObjectFunction))
            appendSection("Other members", node.members.filter {
                it.kind !in setOf(
                        NodeKind.Class,
                        NodeKind.Interface,
                        NodeKind.Enum,
                        NodeKind.Object,
                        NodeKind.AnnotationClass,
                        NodeKind.Exception,
                        NodeKind.Constructor,
                        NodeKind.Property,
                        NodeKind.Package,
                        NodeKind.Function,
                        NodeKind.CompanionObjectProperty,
                        NodeKind.CompanionObjectFunction,
                        NodeKind.ExternalClass,
                        NodeKind.EnumItem
                )
            })

            val allExtensions = collectAllExtensions(node)
            appendSection("Extension Properties", allExtensions.filter { it.kind == NodeKind.Property })
            appendSection("Extension Functions", allExtensions.filter { it.kind == NodeKind.Function })
            appendSection("Companion Object Extension Properties", allExtensions.filter { it.kind == NodeKind.CompanionObjectProperty })
            appendSection("Companion Object Extension Functions", allExtensions.filter { it.kind == NodeKind.CompanionObjectFunction })
            appendSection("Inheritors",
                    node.inheritors.filter { it.kind != NodeKind.EnumItem })
            appendSection("Links", node.links)
        }

        private fun appendSection(caption: String, members: List<DocumentationNode>) {
            if (members.isEmpty()) return

            appendHeader(to, caption, 3)

            val children = members.sortedBy { it.name }
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
                                    appendSummarySignatures(items)
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

        private fun appendSummarySignatures(items: List<DocumentationNode>) {
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
    }

    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val singleNode = nodes.singleOrNull()
        if (singleNode != null) {
            SingleNodePageBuilder(location, to, singleNode).build()
        }
        else {
            PageBuilder(location, to, nodes).build()
        }
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
