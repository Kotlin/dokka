package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode
import java.util.*

data class FormatLink(val text: String, val href: String)

abstract class StructuredOutputBuilder(val to: StringBuilder,
                                       val location: Location,
                                       val locationService: LocationService,
                                       val languageService: LanguageService,
                                       val extension: String,
                                       val impliedPlatforms: List<String>) : FormattedOutputBuilder {

    protected fun wrap(prefix: String, suffix: String, body: () -> Unit) {
        to.append(prefix)
        body()
        to.append(suffix)
    }

    protected fun wrapIfNotEmpty(prefix: String, suffix: String, body: () -> Unit, checkEndsWith: Boolean = false) {
        val startLength = to.length
        to.append(prefix)
        body()
        if (checkEndsWith && to.endsWith(suffix)) {
            to.setLength(to.length - suffix.length)
        } else if (to.length > startLength + prefix.length) {
            to.append(suffix)
        } else {
            to.setLength(startLength)
        }
    }

    protected fun wrapInTag(tag: String,
                            body: () -> Unit,
                            newlineBeforeOpen: Boolean = false,
                            newlineAfterOpen: Boolean = false,
                            newlineAfterClose: Boolean = false) {
        if (newlineBeforeOpen && !to.endsWith('\n')) to.appendln()
        to.append("<$tag>")
        if (newlineAfterOpen) to.appendln()
        body()
        to.append("</$tag>")
        if (newlineAfterClose) to.appendln()
    }

    protected abstract fun ensureParagraph()

    open fun appendSampleBlockCode(language: String, imports: () -> Unit, body: () -> Unit) = appendBlockCode(language, body)
    abstract fun appendBlockCode(language: String, body: () -> Unit)
    abstract fun appendHeader(level: Int = 1, body: () -> Unit)
    abstract fun appendParagraph(body: () -> Unit)

    open fun appendSoftParagraph(body: () -> Unit) {
        ensureParagraph()
        body()
    }

    abstract fun appendLine()
    abstract fun appendAnchor(anchor: String)

    abstract fun appendTable(vararg columns: String, body: () -> Unit)
    abstract fun appendTableBody(body: () -> Unit)
    abstract fun appendTableRow(body: () -> Unit)
    abstract fun appendTableCell(body: () -> Unit)

    abstract fun appendText(text: String)

    open fun appendSinceKotlin(version: String) {
        appendParagraph {
            appendText("Available since Kotlin: ")
            appendCode { appendText(version) }
        }
    }

    open fun appendSectionWithTag(section: ContentSection) {
        appendParagraph {
            appendStrong { appendText(section.tag) }
            appendLine()
            appendContent(section)
        }
    }

    open fun appendSymbol(text: String) {
        appendText(text)
    }

    open fun appendKeyword(text: String) {
        appendText(text)
    }

    open fun appendIdentifier(text: String, kind: IdentifierKind, signature: String?) {
        appendText(text)
    }

    fun appendEntity(text: String) {
        to.append(text)
    }

    abstract fun appendLink(href: String, body: () -> Unit)

    open fun appendLink(link: FormatLink) {
        appendLink(link.href) { appendText(link.text) }
    }

    abstract fun appendStrong(body: () -> Unit)
    abstract fun appendStrikethrough(body: () -> Unit)
    abstract fun appendEmphasis(body: () -> Unit)
    abstract fun appendCode(body: () -> Unit)
    abstract fun appendUnorderedList(body: () -> Unit)
    abstract fun appendOrderedList(body: () -> Unit)
    abstract fun appendListItem(body: () -> Unit)

    abstract fun appendBreadcrumbSeparator()
    abstract fun appendNonBreakingSpace()
    open fun appendSoftLineBreak() {
    }

    open fun appendIndentedSoftLineBreak() {
    }

    fun appendContent(content: List<ContentNode>) {
        for (contentNode in content) {
            appendContent(contentNode)
        }
    }

    open fun appendContent(content: ContentNode) {
        when (content) {
            is ContentText -> appendText(content.text)
            is ContentSymbol -> appendSymbol(content.text)
            is ContentKeyword -> appendKeyword(content.text)
            is ContentIdentifier -> appendIdentifier(content.text, content.kind, content.signature)
            is ContentNonBreakingSpace -> appendNonBreakingSpace()
            is ContentSoftLineBreak -> appendSoftLineBreak()
            is ContentIndentedSoftLineBreak -> appendIndentedSoftLineBreak()
            is ContentEntity -> appendEntity(content.text)
            is ContentStrong -> appendStrong { appendContent(content.children) }
            is ContentStrikethrough -> appendStrikethrough { appendContent(content.children) }
            is ContentCode -> appendCode { appendContent(content.children) }
            is ContentEmphasis -> appendEmphasis { appendContent(content.children) }
            is ContentUnorderedList -> appendUnorderedList { appendContent(content.children) }
            is ContentOrderedList -> appendOrderedList { appendContent(content.children) }
            is ContentListItem -> appendListItem {
                val child = content.children.singleOrNull()
                if (child is ContentParagraph) {
                    appendContent(child.children)
                } else {
                    appendContent(content.children)
                }
            }

            is ContentNodeLink -> {
                val node = content.node
                val linkTo = if (node != null) locationHref(location, node) else "#"
                appendLinkIfNotThisPage(linkTo, content)
            }
            is ContentExternalLink -> appendLinkIfNotThisPage(content.href, content)

            is ContentParagraph -> {
                if (!content.isEmpty()) {
                    appendParagraph { appendContent(content.children) }
                }
            }

            is ContentBlockSampleCode, is ContentBlockCode -> {
                content as ContentBlockCode
                fun ContentBlockCode.appendBlockCodeContent() {
                    children
                            .dropWhile { it is ContentText && it.text.isBlank() }
                            .forEach { appendContent(it) }
                }
                when (content) {
                    is ContentBlockSampleCode ->
                        appendSampleBlockCode(content.language, content.importsBlock::appendBlockCodeContent, { content.appendBlockCodeContent() })
                    is ContentBlockCode ->
                        appendBlockCode(content.language, { content.appendBlockCodeContent() })
                }
            }
            is ContentHeading -> appendHeader(content.level) { appendContent(content.children) }
            is ContentBlock -> appendContent(content.children)
        }
    }

    private fun appendLinkIfNotThisPage(href: String, content: ContentBlock) {
        if (href == ".") {
            appendContent(content.children)
        } else {
            appendLink(href) { appendContent(content.children) }
        }
    }

    open fun link(from: DocumentationNode,
                  to: DocumentationNode,
                  name: (DocumentationNode) -> String = DocumentationNode::name): FormatLink = link(from, to, extension, name)

    open fun link(from: DocumentationNode,
                  to: DocumentationNode,
                  extension: String,
                  name: (DocumentationNode) -> String = DocumentationNode::name): FormatLink {
        if (to.owner?.kind == NodeKind.GroupNode)
            return link(from, to.owner!!, extension, name)

        if (from.owner?.kind == NodeKind.GroupNode)
            return link(from.owner!!, to, extension, name)

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

    protected open fun appendAsSignature(node: ContentNode, block: () -> Unit) {
        block()
    }

    protected open fun appendAsOverloadGroup(to: StringBuilder, platforms: Set<String>, block: () -> Unit) {
        block()
    }

    protected open fun appendIndexRow(platforms: Set<String>, block: () -> Unit) {
        appendTableRow(block)
    }

    protected open fun appendPlatforms(platforms: Set<String>) {
        if (platforms.isNotEmpty()) {
            appendLine()
            appendText(platforms.joinToString(prefix = "(", postfix = ")"))
        }
    }

    protected open fun appendBreadcrumbs(path: Iterable<FormatLink>) {
        for ((index, item) in path.withIndex()) {
            if (index > 0) {
                appendBreadcrumbSeparator()
            }
            appendLink(item)
        }
    }

    fun Content.getSectionsWithSubjects(): Map<String, List<ContentSection>> =
            sections.filter { it.subjectName != null }.groupBy { it.tag }

    private fun ContentNode.appendSignature() {
        if (this is ContentBlock && this.isEmpty()) {
            return
        }

        val signatureAsCode = ContentCode()
        signatureAsCode.append(this)
        appendContent(signatureAsCode)
    }

    open inner class PageBuilder(val nodes: Iterable<DocumentationNode>, val noHeader: Boolean = false) {
        open fun build() {
            val breakdownByLocation = nodes.groupBy { node ->
                node.path.filterNot { it.name.isEmpty() }.map { link(node, it) }.distinct()
            }

            for ((path, nodes) in breakdownByLocation) {
                if (!noHeader && path.isNotEmpty()) {
                    appendBreadcrumbs(path)
                    appendLine()
                    appendLine()
                }
                appendLocation(nodes.filter { it.kind != NodeKind.ExternalClass })
            }
        }

        private fun appendLocation(nodes: Iterable<DocumentationNode>) {
            val singleNode = nodes.singleOrNull()
            if (singleNode != null && singleNode.isModuleOrPackage()) {
                if (singleNode.kind == NodeKind.Package) {
                    val packageName = if (singleNode.name.isEmpty()) "<root>" else singleNode.name
                    appendHeader(2) { appendText("Package $packageName") }
                }
                singleNode.appendPlatforms()
                appendContent(singleNode.content)
            } else {
                val breakdownByName = nodes.groupBy { node -> node.name }
                for ((name, items) in breakdownByName) {
                    if (!noHeader)
                        appendHeader { appendText(name) }
                    appendDocumentation(items, singleNode != null)
                }
            }
        }

        private fun appendDocumentation(overloads: Iterable<DocumentationNode>, isSingleNode: Boolean) {
            val breakdownBySummary = overloads.groupByTo(LinkedHashMap()) { node -> node.content }

            if (breakdownBySummary.size == 1) {
                formatOverloadGroup(breakdownBySummary.values.single(), isSingleNode)
            } else {
                for ((_, items) in breakdownBySummary) {

                    appendAsOverloadGroup(to, platformsOfItems(items)) {
                        formatOverloadGroup(items)
                    }

                }
            }
        }

        private fun formatOverloadGroup(items: List<DocumentationNode>, isSingleNode: Boolean = false) {
            for ((index, item) in items.withIndex()) {
                if (index > 0) appendLine()
                val rendered = languageService.render(item)
                item.detailOrNull(NodeKind.Signature)?.let {
                    if (item.kind !in NodeKind.classLike || !isSingleNode)
                        appendAnchor(it.name)
                }
                appendAsSignature(rendered) {
                    appendCode { appendContent(rendered) }
                    item.appendSourceLink()
                }
                item.appendOverrides()
                item.appendDeprecation()
                item.appendPlatforms()
            }
            // All items have exactly the same documentation, so we can use any item to render it
            val item = items.first()
            item.details(NodeKind.OverloadGroupNote).forEach {
                appendContent(it.content)
            }

            appendContent(item.content.summary)
            item.appendDescription()
        }

        private fun DocumentationNode.appendSourceLink() {
            val sourceUrl = details(NodeKind.SourceUrl).firstOrNull()
            if (sourceUrl != null) {
                to.append(" ")
                appendLink(sourceUrl.name) { to.append("(source)") }
            }
        }

        private fun DocumentationNode.appendOverrides() {
            overrides.forEach {
                appendParagraph {
                    to.append("Overrides ")
                    val location = locationService.relativePathToLocation(this, it)
                    appendLink(FormatLink(it.owner!!.name + "." + it.name, location))
                }
            }
        }

        private fun DocumentationNode.appendDeprecation() {
            if (deprecation != null) {
                val deprecationParameter = deprecation!!.details(NodeKind.Parameter).firstOrNull()
                val deprecationValue = deprecationParameter?.details(NodeKind.Value)?.firstOrNull()
                appendLine()
                if (deprecationValue != null) {
                    appendStrong { to.append("Deprecated:") }
                    appendText(" " + deprecationValue.name.removeSurrounding("\""))
                    appendLine()
                    appendLine()
                } else if (deprecation?.content != Content.Empty) {
                    appendStrong { to.append("Deprecated:") }
                    to.append(" ")
                    appendContent(deprecation!!.content)
                } else {
                    appendStrong { to.append("Deprecated") }
                    appendLine()
                    appendLine()
                }
            }
        }

        private fun DocumentationNode.appendPlatforms() {
            val platforms = if (isModuleOrPackage())
                platformsToShow.toSet() + platformsOfItems(members)
            else
                platformsToShow

            if(platforms.isEmpty()) return

            appendParagraph {
                appendStrong { to.append("Platform and version requirements:") }
                to.append(" " + platforms.joinToString())
            }
        }

        protected fun platformsOfItems(items: List<DocumentationNode>): Set<String> {
            val platforms = items.asSequence().map {
                when (it.kind) {
                    NodeKind.ExternalClass, NodeKind.Package, NodeKind.Module, NodeKind.GroupNode -> platformsOfItems(it.members)
                    else -> it.platformsToShow.toSet()
                }
            }

            fun String.isKotlinVersion() = this.startsWith("Kotlin")

            // Calculating common platforms for items
            return platforms.reduce { result, platformsOfItem ->
                val otherKotlinVersion = result.find { it.isKotlinVersion() }
                val (kotlinVersions, otherPlatforms) = platformsOfItem.partition { it.isKotlinVersion() }

                // When no Kotlin version specified, it means that version is 1.0
                if (otherKotlinVersion != null && kotlinVersions.isNotEmpty()) {
                    val allKotlinVersions = (kotlinVersions + otherKotlinVersion).distinct()

                    val minVersion = allKotlinVersions.min()!!
                    val resultVersion = when {
                        allKotlinVersions.size == 1 -> allKotlinVersions.single()
                        minVersion.endsWith("+") -> minVersion
                        else -> minVersion + "+"
                    }

                    result.intersect(otherPlatforms) + resultVersion
                } else {
                    result.intersect(platformsOfItem)
                }
            }
        }

        val DocumentationNode.platformsToShow: List<String>
            get() = platforms.let { if (it.containsAll(impliedPlatforms)) it - impliedPlatforms else it }

        private fun DocumentationNode.appendDescription() {
            if (content.description != ContentEmpty) {
                appendContent(content.description)
            }
            content.getSectionsWithSubjects().forEach {
                appendSectionWithSubject(it.key, it.value)
            }

            for (section in content.sections.filter { it.subjectName == null }) {
                appendSectionWithTag(section)
            }
        }

        fun appendSectionWithSubject(title: String, subjectSections: List<ContentSection>) {
            appendHeader(3) { appendText(title) }
            subjectSections.forEach {
                val subjectName = it.subjectName
                if (subjectName != null) {
                    appendSoftParagraph {
                        appendAnchor(subjectName)
                        appendCode { to.append(subjectName) }
                        to.append(" - ")
                        appendContent(it)
                    }
                }
            }
        }
    }

    inner class GroupNodePageBuilder(val node: DocumentationNode) : PageBuilder(listOf(node)) {

        override fun build() {
            val breakdownByLocation = node.path.filterNot { it.name.isEmpty() }.map { link(node, it) }

            appendBreadcrumbs(breakdownByLocation)
            appendLine()
            appendLine()
            appendHeader { appendText(node.name) }

            fun DocumentationNode.priority(): Int = when (kind) {
                NodeKind.TypeAlias -> 1
                NodeKind.Class -> 2
                else -> 3
            }

            for (member in node.members.sortedBy(DocumentationNode::priority)) {

                appendAsOverloadGroup(to, platformsOfItems(listOf(member))) {
                    formatSubNodeOfGroup(member)
                }

            }
        }

        fun formatSubNodeOfGroup(member: DocumentationNode) {
            SingleNodePageBuilder(member, true).build()
        }
    }

    inner class SingleNodePageBuilder(val node: DocumentationNode, noHeader: Boolean = false)
        : PageBuilder(listOf(node), noHeader) {

        override fun build() {
            super.build()

            if (node.kind == NodeKind.ExternalClass) {
                appendSection("Extensions for ${node.name}", node.members)
                return
            }

            fun DocumentationNode.membersOrGroupMembers(predicate: (DocumentationNode) -> Boolean): List<DocumentationNode> {
                return members.filter(predicate) + members(NodeKind.GroupNode).flatMap { it.members.filter(predicate) }
            }

            fun DocumentationNode.membersOrGroupMembers(kind: NodeKind): List<DocumentationNode> {
                return membersOrGroupMembers { it.kind == kind }
            }

            appendSection("Packages", node.members(NodeKind.Package), platformsBasedOnMembers = true)
            appendSection("Types", node.membersOrGroupMembers { it.kind in NodeKind.classLike && it.kind != NodeKind.TypeAlias && it.kind != NodeKind.AnnotationClass && it.kind != NodeKind.Exception })
            appendSection("Annotations", node.membersOrGroupMembers(NodeKind.AnnotationClass))
            appendSection("Exceptions", node.membersOrGroupMembers(NodeKind.Exception))
            appendSection("Type Aliases", node.membersOrGroupMembers(NodeKind.TypeAlias))
            appendSection("Extensions for External Classes", node.members(NodeKind.ExternalClass))
            appendSection("Enum Values", node.members(NodeKind.EnumItem), sortMembers = false, omitSamePlatforms = true)
            appendSection("Constructors", node.members(NodeKind.Constructor), omitSamePlatforms = true)
            appendSection("Properties", node.members(NodeKind.Property), omitSamePlatforms = true)
            appendSection("Inherited Properties", node.inheritedMembers(NodeKind.Property))
            appendSection("Functions", node.members(NodeKind.Function), omitSamePlatforms = true)
            appendSection("Inherited Functions", node.inheritedMembers(NodeKind.Function))
            appendSection("Companion Object Properties", node.members(NodeKind.CompanionObjectProperty), omitSamePlatforms = true)
            appendSection("Inherited Companion Object Properties", node.inheritedCompanionObjectMembers(NodeKind.Property))
            appendSection("Companion Object Functions", node.members(NodeKind.CompanionObjectFunction), omitSamePlatforms = true)
            appendSection("Inherited Companion Object Functions", node.inheritedCompanionObjectMembers(NodeKind.Function))
            appendSection("Other members", node.members.filter {
                it.kind !in setOf(
                        NodeKind.Class,
                        NodeKind.Interface,
                        NodeKind.Enum,
                        NodeKind.Object,
                        NodeKind.AnnotationClass,
                        NodeKind.Exception,
                        NodeKind.TypeAlias,
                        NodeKind.Constructor,
                        NodeKind.Property,
                        NodeKind.Package,
                        NodeKind.Function,
                        NodeKind.CompanionObjectProperty,
                        NodeKind.CompanionObjectFunction,
                        NodeKind.ExternalClass,
                        NodeKind.EnumItem,
                        NodeKind.AllTypes,
                        NodeKind.GroupNode
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
                appendHeader(3) { to.append("Index") }
                node.members(NodeKind.AllTypes).singleOrNull()?.let { allTypes ->
                    appendLink(link(node, allTypes, { "All Types" }))
                }
            }
        }

        private fun appendSection(caption: String, members: List<DocumentationNode>,
                                  sortMembers: Boolean = true,
                                  omitSamePlatforms: Boolean = false,
                                  platformsBasedOnMembers: Boolean = false) {
            if (members.isEmpty()) return

            appendHeader(3) { appendText(caption) }

            val children = if (sortMembers) members.sortedBy { it.name } else members
            val membersMap = children.groupBy { link(node, it) }



            appendTable("Name", "Summary") {
                appendTableBody {
                    for ((memberLocation, members) in membersMap) {
                        val elementPlatforms = platformsOfItems(members, omitSamePlatforms)
                        val platforms = if (platformsBasedOnMembers)
                            members.flatMapTo(mutableSetOf()) { platformsOfItems(it.members) } + elementPlatforms
                        else
                            elementPlatforms
                        appendIndexRow(platforms) {
                            appendTableCell {
                                appendParagraph {
                                    appendLink(memberLocation)
                                    if (members.singleOrNull()?.kind != NodeKind.ExternalClass) {
                                        appendPlatforms(platforms)
                                    }
                                }
                            }
                            appendTableCell {
                                val breakdownBySummary = members.groupBy { it.summary }
                                for ((summary, items) in breakdownBySummary) {
                                    appendSummarySignatures(items)
                                    appendContent(summary)
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun platformsOfItems(items: List<DocumentationNode>, omitSamePlatforms: Boolean = true): Set<String> {
            val platforms = platformsOfItems(items)
            if (platforms.isNotEmpty() && (platforms != node.platformsToShow.toSet() || !omitSamePlatforms)) {
                return platforms
            }
            return emptySet()
        }

        private fun appendSummarySignatures(items: List<DocumentationNode>) {
            val summarySignature = languageService.summarizeSignatures(items)
            if (summarySignature != null) {
                appendAsSignature(summarySignature) {
                    summarySignature.appendSignature()
                }
                return
            }
            val renderedSignatures = items.map { languageService.render(it, RenderMode.SUMMARY) }
            renderedSignatures.subList(0, renderedSignatures.size - 1).forEach {
                appendAsSignature(it) {
                    it.appendSignature()
                }
                appendLine()
            }
            appendAsSignature(renderedSignatures.last()) {
                renderedSignatures.last().appendSignature()
            }
        }
    }

    inner class AllTypesNodeBuilder(val node: DocumentationNode)
        : PageBuilder(listOf(node)) {

        override fun build() {
            appendContent(node.owner!!.summary)
            appendHeader(3) { to.append("All Types") }

            appendTable("Name", "Summary") {
                appendTableBody {
                    for (type in node.members) {
                        appendTableRow {
                            appendTableCell {
                                appendLink(link(node, type) {
                                    if (it.kind == NodeKind.ExternalClass) it.name else it.qualifiedName()
                                })
                                if (type.kind == NodeKind.ExternalClass) {
                                    val packageName = type.owner?.name
                                    if (packageName != null) {
                                        appendText(" (extensions in package $packageName)")
                                    }
                                }
                            }
                            appendTableCell {
                                appendContent(type.summary)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun appendNodes(nodes: Iterable<DocumentationNode>) {
        val singleNode = nodes.singleOrNull()
        when (singleNode?.kind) {
            NodeKind.AllTypes -> AllTypesNodeBuilder(singleNode).build()
            NodeKind.GroupNode -> GroupNodePageBuilder(singleNode).build()
            null -> PageBuilder(nodes).build()
            else -> SingleNodePageBuilder(singleNode).build()
        }
    }
}

abstract class StructuredFormatService(locationService: LocationService,
                                       val languageService: LanguageService,
                                       override val extension: String,
                                       override final val linkExtension: String = extension) : FormatService {
    val locationService: LocationService = locationService.withExtension(linkExtension)
}
