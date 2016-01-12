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
    abstract fun formatIdentifier(text: String, kind: IdentifierKind, signature: String?): String
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

    fun formatText(location: Location, content: ContentNode, listKind: ListKind = ListKind.Unordered): String {
        return StringBuilder().apply { formatText(location, content, this, listKind) }.toString()
    }

    open fun formatText(location: Location, content: ContentNode, to: StringBuilder, listKind: ListKind = ListKind.Unordered) {
        when (content) {
            is ContentText -> to.append(formatText(content.text))
            is ContentSymbol -> to.append(formatSymbol(content.text))
            is ContentKeyword -> to.append(formatKeyword(content.text))
            is ContentIdentifier -> to.append(formatIdentifier(content.text, content.kind, content.signature))
            is ContentNonBreakingSpace -> to.append(formatNonBreakingSpace())
            is ContentSoftLineBreak -> to.append(formatSoftLineBreak())
            is ContentIndentedSoftLineBreak -> to.append(formatIndentedSoftLineBreak())
            is ContentEntity -> to.append(formatEntity(content.text))
            is ContentStrong -> to.append(formatStrong(formatText(location, content.children)))
            is ContentStrikethrough -> to.append(formatStrikethrough(formatText(location, content.children)))
            is ContentCode -> to.append(formatCode(formatText(location, content.children)))
            is ContentEmphasis -> to.append(formatEmphasis(formatText(location, content.children)))
            is ContentUnorderedList -> to.append(formatUnorderedList(formatText(location, content.children, ListKind.Unordered)))
            is ContentOrderedList -> to.append(formatOrderedList(formatText(location, content.children, ListKind.Ordered)))
            is ContentListItem -> to.append(formatListItem(formatText(location, content.children), listKind))

            is ContentNodeLink -> {
                val node = content.node
                val linkTo = if (node != null) locationHref(location, node) else "#"
                val linkText = formatText(location, content.children)
                if (linkTo == ".") {
                    to.append(linkText)
                } else {
                    to.append(formatLink(linkText, linkTo))
                }
            }
            is ContentExternalLink -> {
                val linkText = formatText(location, content.children)
                if (content.href == ".") {
                    to.append(linkText)
                } else {
                    to.append(formatLink(linkText, content.href))
                }
            }
            is ContentParagraph -> appendParagraph(to, formatText(location, content.children))
            is ContentBlockCode -> appendBlockCode(to, formatText(location, content.children), content.language)
            is ContentHeading -> appendHeader(to, formatText(location, content.children), content.level)
            is ContentBlock -> to.append(formatText(location, content.children))
        }
    }

    open fun link(from: DocumentationNode,
                  to: DocumentationNode,
                  name: (DocumentationNode) -> String = DocumentationNode::name): FormatLink = link(from, to, extension, name)

    open fun link(from: DocumentationNode,
                  to: DocumentationNode,
                  extension: String,
                  name: (DocumentationNode) -> String = DocumentationNode::name): FormatLink {
        return FormatLink(name(to), locationService.relativePathToLocation(from, to))
    }

    fun locationHref(from: Location, to: DocumentationNode): String {
        val topLevelPage = to.references(RefKind.TopLevelPage).singleOrNull()?.to
        if (topLevelPage != null) {
            val signature = to.detailOrNull(NodeKind.Signature)
            return from.relativePathTo(locationService.location(topLevelPage), signature?.name ?: to.name)
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
                formatText(location, singleNode.content, to)
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

        private fun formatOverloadGroup(items: List<DocumentationNode>) {
            items.forEach {
                val rendered = languageService.render(it)
                it.detailOrNull(NodeKind.Signature)?.let {
                    appendAnchor(to, it.name)
                }
                appendAsSignature(to, rendered) {
                    to.append(formatCode(formatText(location, rendered)))
                    it.appendSourceLink()
                }
                it.appendOverrides()
                it.appendDeprecation()
            }
            // All items have exactly the same documentation, so we can use any item to render it
            val item = items.first()
            item.details(NodeKind.OverloadGroupNote).forEach {
                formatText(location, it.content, to)
            }
            formatText(location, item.content.summary, to)
            item.appendDescription()
            appendLine(to)
            appendLine(to)
        }

        private fun DocumentationNode.appendSourceLink() {
            val sourceUrl = details(NodeKind.SourceUrl).firstOrNull()
            if (sourceUrl != null) {
                to.append(" ")
                appendLine(to, formatLink("(source)", sourceUrl.name))
            } else {
                appendLine(to)
            }
        }

        private fun DocumentationNode.appendOverrides() {
            overrides.forEach {
                to.append("Overrides ")
                val location = locationService.relativePathToLocation(this, it)
                appendLine(to, formatLink(FormatLink(it.owner!!.name + "." + it.name, location)))
            }
        }

        private fun DocumentationNode.appendDeprecation() {
            if (deprecation != null) {
                val deprecationParameter = deprecation!!.details(NodeKind.Parameter).firstOrNull()
                val deprecationValue = deprecationParameter?.details(NodeKind.Value)?.firstOrNull()
                if (deprecationValue != null) {
                    to.append(formatStrong("Deprecated:")).append(" ")
                    appendLine(to, formatText(deprecationValue.name.removeSurrounding("\"")))
                    appendLine(to)
                } else if (deprecation?.content != Content.Empty) {
                    to.append(formatStrong("Deprecated:")).append(" ")
                    formatText(location, deprecation!!.content, to)
                } else {
                    appendLine(to, formatStrong("Deprecated"))
                    appendLine(to)
                }
            }
        }

        private fun DocumentationNode.appendDescription() {
            if (content.description != ContentEmpty) {
                appendLine(to, formatText(location, content.description))
                appendLine(to)
            }
            content.getSectionsWithSubjects().forEach {
                appendSectionWithSubject(it.key, it.value)
            }

            for (section in content.sections.filter { it.subjectName == null }) {
                appendLine(to, formatStrong(formatText(section.tag)))
                appendLine(to, formatText(location, section))
            }
        }

        fun appendSectionWithSubject(title: String, subjectSections: List<ContentSection>) {
            appendHeader(to, title, 3)
            var first: Boolean = true
            subjectSections.forEach {
                val subjectName = it.subjectName
                if (subjectName != null) {
                    if (first) {
                        first = false
                    }
                    else {
                        appendLine(to)
                    }

                    appendAnchor(to, subjectName)
                    to.append(formatCode(subjectName)).append(" - ")
                    formatText(location, it, to)
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
            appendSection("Inherited Companion Object Properties", node.inheritedCompanionObjectMembers(NodeKind.Property))
            appendSection("Companion Object Functions", node.members(NodeKind.CompanionObjectFunction))
            appendSection("Inherited Companion Object Functions", node.inheritedCompanionObjectMembers(NodeKind.Function))
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
                        NodeKind.EnumItem,
                        NodeKind.AllTypes
                )
            })

            val allExtensions = node.extensions
            appendSection("Extension Properties", allExtensions.filter { it.kind == NodeKind.Property })
            appendSection("Extension Functions", allExtensions.filter { it.kind == NodeKind.Function })
            appendSection("Companion Object Extension Properties", allExtensions.filter { it.kind == NodeKind.CompanionObjectProperty })
            appendSection("Companion Object Extension Functions", allExtensions.filter { it.kind == NodeKind.CompanionObjectFunction })
            appendSection("Inheritors",
                    node.inheritors.filter { it.kind != NodeKind.EnumItem })

            if (node.kind == NodeKind.Module) {
                appendHeader(to, "Index", 3)
                node.members(NodeKind.AllTypes).singleOrNull()?.let { allTypes ->
                    to.append(formatLink(link(node, allTypes, { "All Types" })))
                }
            }
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

    inner class AllTypesNodeBuilder(location: Location, to: StringBuilder, val node: DocumentationNode)
        : PageBuilder(location, to, listOf(node)) {

        override fun build() {
            to.append(formatText(location, node.owner!!.summary))
            appendHeader(to, "All Types", 3)

            appendTable(to) {
                appendTableBody(to) {
                    for (type in node.members) {
                        appendTableRow(to) {
                            appendTableCell(to) {
                                to.append(formatLink(link(node, type) {
                                    if (it.kind == NodeKind.ExternalClass) it.name else it.qualifiedName()
                                }))
                                if (type.kind == NodeKind.ExternalClass) {
                                    val packageName = type.owner?.name
                                    if (packageName != null) {
                                        to.append(formatText(" (extensions in package $packageName)"))
                                    }
                                }
                            }
                            appendTableCell(to) {
                                to.append(formatText(location, type.summary))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val singleNode = nodes.singleOrNull()
        if (singleNode != null) {
            if (singleNode.kind == NodeKind.AllTypes) {
                AllTypesNodeBuilder(location, to, singleNode).build()
            }
            else {
                SingleNodePageBuilder(location, to, singleNode).build()
            }
        }
        else {
            PageBuilder(location, to, nodes).build()
        }
    }
}
