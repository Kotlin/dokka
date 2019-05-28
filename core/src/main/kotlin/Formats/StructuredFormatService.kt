package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

data class FormatLink(val text: String, val href: String)

private data class Summarized(
    val data: List<SummarizedBySummary>
) {

    constructor(data: Map<ContentNode, Map<ContentNode, List<DocumentationNode>>>) : this(
        data.entries.map { (summary, signatureToMember) ->
            SummarizedBySummary(
                summary,
                signatureToMember.map { (signature, nodes) ->
                    SummarizedNodes(signature, nodes)
                }
            )
        }
    )

    data class SummarizedNodes(val content: ContentNode, val nodes: List<DocumentationNode>) {
        val platforms = effectivePlatformsForMembers(nodes)
    }
    data class SummarizedBySummary(val content: ContentNode, val signatures: List<SummarizedNodes>) {
        val platforms = effectivePlatformsForMembers(signatures.flatMap { it.nodes })
        val platformsOnSignature = !samePlatforms(signatures.map { it.platforms })
    }


    fun computePlatformLevel(): PlatformPlacement {
        if (data.any { it.platformsOnSignature }) {
            return PlatformPlacement.Signature
        }
        if (samePlatforms(data.map { it.platforms })) {
            return PlatformPlacement.Row
        }
        return PlatformPlacement.Summary
    }
    val platformPlacement: PlatformPlacement = computePlatformLevel()
    val platforms = effectivePlatformsForMembers(data.flatMap { it.signatures.flatMap { it.nodes } })


    enum class PlatformPlacement {
        Row, Summary, Signature
    }
}

abstract class StructuredOutputBuilder(val to: StringBuilder,
                                       val location: Location,
                                       val generator: NodeLocationAwareGenerator,
                                       val languageService: LanguageService,
                                       val extension: String,
                                       impliedPlatforms: List<String>) : FormattedOutputBuilder {

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
            appendText("Since: ")
            appendCode { appendText(version) }
    }

    open fun appendSectionWithTag(section: ContentSection) {
        appendParagraph {
            appendStrong { appendText(section.tag) }
            appendLine()
            appendContent(section)
        }
    }

    open fun appendAsPlatformDependentBlock(platforms: PlatformsData, block: (PlatformsData) -> Unit) {
        block(platforms)
    }

    open fun appendAsSummaryGroup(platforms: PlatformsData, block: (PlatformsData) -> Unit) {
        appendAsPlatformDependentBlock(platforms, block)
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

    open fun appendAsNodeDescription(platforms: PlatformsData, block: () -> Unit) {
        block()
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

            is NodeRenderContent -> {
                val node = content.node
                appendContent(languageService.render(node, content.mode))
            }
            is ContentNodeLink -> {
                val node = content.node
                val linkTo = if (node != null) locationHref(location, node, generator) else "#"
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
                        appendSampleBlockCode(content.language, content.importsBlock::appendBlockCodeContent) { content.appendBlockCodeContent() }
                    is ContentBlockCode ->
                        appendBlockCode(content.language) { content.appendBlockCodeContent() }
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

    open fun link(
        from: DocumentationNode,
        to: DocumentationNode,
        name: (DocumentationNode) -> String = DocumentationNode::name
    ): FormatLink = link(from, to, extension, name)

    open fun link(
        from: DocumentationNode,
        to: DocumentationNode,
        extension: String,
        name: (DocumentationNode) -> String = DocumentationNode::name
    ): FormatLink =
        FormatLink(name(to), generator.relativePathToLocation(from, to))

    private fun DocumentationNode.isModuleOrPackage(): Boolean =
        kind == NodeKind.Module || kind == NodeKind.Package

    protected open fun appendAsSignature(node: ContentNode, block: () -> Unit) {
        block()
    }

    protected open fun appendAsOverloadGroup(to: StringBuilder, platforms: PlatformsData, block: () -> Unit) {
        block()
    }

    protected open fun appendIndexRow(platforms: PlatformsData, block: () -> Unit) {
        appendTableRow(block)
    }

    protected open fun appendPlatformsAsText(platforms: PlatformsData) {
        appendPlatforms(platforms)
    }

    protected open fun appendPlatforms(platforms: PlatformsData) {
        if (platforms.isNotEmpty()) {
            appendText(platforms.keys.joinToString(prefix = "(", postfix = ") "))
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
            val breakdownBySummary = overloads.groupByTo(LinkedHashMap()) { node ->
                when (node.kind) {
                    NodeKind.GroupNode -> node.origins.map { it.content }
                    else -> node.content
                }
            }

            if (breakdownBySummary.size == 1) {
                val node = breakdownBySummary.values.single()
                appendAsNodeDescription(effectivePlatformsForMembers(node)) {
                    formatOverloadGroup(node, isSingleNode)
                }
            } else {
                for ((_, items) in breakdownBySummary) {
                    appendAsOverloadGroup(to, effectivePlatformsForMembers(items)) {
                        formatOverloadGroup(items)
                    }
                }
            }
        }

        private fun formatOverloadGroup(items: List<DocumentationNode>, isSingleNode: Boolean = false) {

            val platformsPerGroup = samePlatforms(
                items.flatMap {
                    if (it.kind == NodeKind.GroupNode) {
                        it.origins.groupBy { origin ->
                            languageService.render(origin)
                        }.values.map { origins -> effectivePlatformsForMembers(origins) }
                    } else {
                        listOf(effectivePlatformsForNode(it))
                    }
                }
            )

            if (platformsPerGroup) {
                appendAsPlatformDependentBlock(effectivePlatformsForMembers(items)) { platforms ->
                    appendPlatforms(platforms)
                }
            }
            for ((index, item) in items.withIndex()) {
                if (index > 0) appendLine()

                if (item.kind == NodeKind.GroupNode) {
                    renderGroupNode(item, isSingleNode, !platformsPerGroup)
                } else {
                    renderSimpleNode(item, isSingleNode, !platformsPerGroup)
                }

            }
            // All items have exactly the same documentation, so we can use any item to render it
            val item = items.first()
            // TODO: remove this block cause there is no one node with OverloadGroupNote detail
            item.details(NodeKind.OverloadGroupNote).forEach {
                appendContent(it.content)
            }

            if (item.kind == NodeKind.GroupNode) {
                val groupByContent = item.origins.groupBy { it.content }
                if (groupByContent.count { !it.key.isEmpty() } > 1) {
                    if (groupByContent.size > 1) println("[mult] Found ov diff: ${generator.location(item).path}")
                }
                for ((content, origins) in groupByContent) {
                    if (content.isEmpty()) continue
                    appendAsPlatformDependentBlock(effectivePlatformsForMembers(origins)) { platforms ->
                        if (groupByContent.count { !it.key.isEmpty() } > 1) {
                            appendPlatformsAsText(platforms)
                        }
                        appendContent(content.summary)
                        content.appendDescription()
                    }
                }
            } else {
                val platforms = effectivePlatformsForNode(item)
                appendAsPlatformDependentBlock(platforms) {
                    appendContent(item.summary)
                    item.content.appendDescription()
                }
            }
        }


        fun renderSimpleNode(item: DocumentationNode, isSingleNode: Boolean, withPlatforms: Boolean = true) {
            appendAsPlatformDependentBlock(effectivePlatformsForMembers(listOf(item))) { platforms ->
                // TODO: use summarizesignatures
                val rendered = languageService.render(item)
                item.detailOrNull(NodeKind.Signature)?.let {
                    if (item.kind !in NodeKind.classLike || !isSingleNode)
                        appendAnchor(it.name)
                }
                if (withPlatforms) {
                    appendPlatforms(platforms)
                }
                appendAsSignature(rendered) {
                    appendCode { appendContent(rendered) }
                    item.appendSourceLink()
                }
                item.appendOverrides()
                item.appendDeprecation()
            }
        }

        fun renderGroupNode(item: DocumentationNode, isSingleNode: Boolean, withPlatforms: Boolean = true) {
            // TODO: use summarizesignatures
            val groupBySignature = item.origins.groupBy {
                languageService.render(it)
            }

            for ((sign, nodes) in groupBySignature) {
                appendAsPlatformDependentBlock(effectivePlatformsForMembers(nodes)) { platforms ->
                    val first = nodes.first()
                    first.detailOrNull(NodeKind.Signature)?.let {
                        if (item.kind !in NodeKind.classLike || !isSingleNode)
                            appendAnchor(it.name)
                    }

                    if (withPlatforms) {
                        appendPlatforms(platforms)
                    }

                    appendAsSignature(sign) {
                        appendCode { appendContent(sign) }
                    }
                    first.appendOverrides()
                    first.appendDeprecation()
                }

            }
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
                    val location = generator.relativePathToLocation(this, it)

                    appendLink(FormatLink(it.owner!!.name + "." + it.name, location))
                }
            }
        }

        private fun DocumentationNode.appendDeprecation() {
            if (deprecation != null) {
                val deprecationParameter = deprecation!!.details(NodeKind.Parameter).firstOrNull()
                val deprecationValue = deprecationParameter?.details(NodeKind.Value)?.firstOrNull()
                appendLine()
                when {
                    deprecationValue != null -> {
                        appendStrong { to.append("Deprecated:") }
                        appendText(" " + deprecationValue.name.removeSurrounding("\""))
                        appendLine()
                        appendLine()
                    }
                    deprecation?.content != Content.Empty -> {
                        appendStrong { to.append("Deprecated:") }
                        to.append(" ")
                        appendContent(deprecation!!.content)
                    }
                    else -> {
                        appendStrong { to.append("Deprecated") }
                        appendLine()
                        appendLine()
                    }
                }
            }
        }


//        protected fun platformsOfItems(items: List<DocumentationNode>): Set<String> {
//            val platforms = items.asSequence().map {
//                when (it.kind) {
//                    NodeKind.ExternalClass, NodeKind.Package, NodeKind.Module -> platformsOfItems(it.members)
//                    NodeKind.GroupNode -> platformsOfItems(it.origins)
//                    else -> it.platformsToShow.toSet()
//                }
//            }
//
//            fun String.isKotlinVersion() = this.startsWith("Kotlin")
//
//            if (platforms.count() == 0) return emptySet()
//
//            // Calculating common platforms for items
//            return platforms.reduce { result, platformsOfItem ->
//                val otherKotlinVersion = result.find { it.isKotlinVersion() }
//                val (kotlinVersions, otherPlatforms) = platformsOfItem.partition { it.isKotlinVersion() }
//
//                // When no Kotlin version specified, it means that version is 1.0
//                if (otherKotlinVersion != null && kotlinVersions.isNotEmpty()) {
//                    result.intersect(platformsOfItem) + mergeVersions(otherKotlinVersion, kotlinVersions)
//                } else {
//                    result.intersect(platformsOfItem)
//                }
//            }
//        }
//
//        protected fun unionPlatformsOfItems(items: List<DocumentationNode>): Set<String> {
//            val platforms = items.asSequence().map {
//                when (it.kind) {
//                    NodeKind.GroupNode -> unionPlatformsOfItems(it.origins)
//                    else -> it.platformsToShow.toSet()
//                }
//            }
//
//            fun String.isKotlinVersion() = this.startsWith("Kotlin")
//
//            if (platforms.count() == 0) return emptySet()
//
//            // Calculating common platforms for items
//            return platforms.reduce { result, platformsOfItem ->
//                val otherKotlinVersion = result.find { it.isKotlinVersion() }
//                val (kotlinVersions, otherPlatforms) = platformsOfItem.partition { it.isKotlinVersion() }
//
//                // When no Kotlin version specified, it means that version is 1.0
//                if (otherKotlinVersion != null && kotlinVersions.isNotEmpty()) {
//                    result.union(otherPlatforms) + mergeVersions(otherKotlinVersion, kotlinVersions)
//                } else {
//                    result.union(otherPlatforms)
//                }
//            }
//        }

//        val DocumentationNode.platformsToShow: List<String>
//            get() = platforms

        private fun Content.appendDescription() {
            if (description != ContentEmpty) {
                appendContent(description)
            }


            getSectionsWithSubjects().forEach {
                appendSectionWithSubject(it.key, it.value)
            }

            for (section in sections.filter { it.subjectName == null }) {
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

        fun appendOriginsGroupByContent(node: DocumentationNode) {
            require(node.kind == NodeKind.GroupNode)
            val groupByContent =
                node.origins.groupBy { it.content }
                    .mapValues { (_, origins) ->
                        effectivePlatformsForMembers(origins)
                    }
                    .filterNot { it.key.isEmpty() }
                    .toList()
                    .sortedByDescending { it.second.size }

            if (groupByContent.size > 1) println("[mult] Found diff: ${generator.location(node).path}")
            for ((content, platforms) in groupByContent) {
                appendAsPlatformDependentBlock(platforms) {
                    if (groupByContent.size > 1) {
                        appendPlatformsAsText(platforms)
                    }
                    appendContent(content.summary)
                    content.appendDescription()
                }
            }
        }
    }

    inner class SingleNodePageBuilder(val node: DocumentationNode, noHeader: Boolean = false) :
        PageBuilder(listOf(node), noHeader) {

        override fun build() {
            super.build()
            SectionsBuilder(node).build()
        }
    }

    inner class GroupNodePageBuilder(val node: DocumentationNode) : PageBuilder(listOf(node)) {

        override fun build() {
            val breakdownByLocation = node.path.filterNot { it.name.isEmpty() }.map { link(node, it) }

            appendBreadcrumbs(breakdownByLocation)
            appendLine()
            appendLine()
            appendHeader { appendText(node.name) }

            appendAsNodeDescription(effectivePlatformsForNode(node)) {
                renderGroupNode(node, true)

                appendOriginsGroupByContent(node)
            }

            SectionsBuilder(node).build()
        }
    }
//
//    private fun unionPlatformsOfItems(items: List<DocumentationNode>): Set<String> {
//        val platforms = items.flatMapTo(mutableSetOf<String>()) {
//            when (it.kind) {
//                NodeKind.GroupNode -> unionPlatformsOfItems(it.origins)
//                else -> it.platforms
//            }
//        }
//
//        return platforms
//    }


    inner class SectionsBuilder(val node: DocumentationNode): PageBuilder(listOf(node)) {
        override fun build() {
            if (node.kind == NodeKind.ExternalClass) {
                appendSection("Extensions for ${node.name}", node.members)
                return
            }

            fun DocumentationNode.membersOrGroupMembers(predicate: (DocumentationNode) -> Boolean): List<DocumentationNode> {
                return members.filter(predicate) + members(NodeKind.GroupNode).filter{ it.origins.isNotEmpty() && predicate(it.origins.first()) }
            }

            fun DocumentationNode.membersOrGroupMembers(kind: NodeKind): List<DocumentationNode> {
                return membersOrGroupMembers { it.kind == kind }
            }

            appendSection("Packages", node.members(NodeKind.Package), platformsBasedOnMembers = true)
            appendSection("Types", node.membersOrGroupMembers { it.kind in NodeKind.classLike /*&& it.kind != NodeKind.TypeAlias*/ && it.kind != NodeKind.AnnotationClass && it.kind != NodeKind.Exception })
            appendSection("Annotations", node.membersOrGroupMembers(NodeKind.AnnotationClass))
            appendSection("Exceptions", node.membersOrGroupMembers(NodeKind.Exception))
            appendSection("Extensions for External Classes", node.members(NodeKind.ExternalClass))
            appendSection("Enum Values", node.membersOrGroupMembers(NodeKind.EnumItem), sortMembers = false, omitSamePlatforms = true)
            appendSection("Constructors", node.membersOrGroupMembers(NodeKind.Constructor), omitSamePlatforms = true)
            appendSection("Properties", node.membersOrGroupMembers(NodeKind.Property), omitSamePlatforms = true)
            appendSection("Inherited Properties", node.inheritedMembers(NodeKind.Property))
            appendSection("Functions", node.membersOrGroupMembers(NodeKind.Function), omitSamePlatforms = true)
            appendSection("Inherited Functions", node.inheritedMembers(NodeKind.Function))
            appendSection("Companion Object Properties", node.membersOrGroupMembers(NodeKind.CompanionObjectProperty), omitSamePlatforms = true)
            appendSection("Inherited Companion Object Properties", node.inheritedCompanionObjectMembers(NodeKind.Property))
            appendSection("Companion Object Functions", node.membersOrGroupMembers(NodeKind.CompanionObjectFunction), omitSamePlatforms = true)
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
                    appendLink(link(node, allTypes) { "All Types" })
                }
            }
        }

        private fun appendSection(caption: String, members: List<DocumentationNode>,
                                  sortMembers: Boolean = true,
                                  omitSamePlatforms: Boolean = false,
                                  platformsBasedOnMembers: Boolean = false) {
            if (members.isEmpty()) return

            appendHeader(3) { appendText(caption) }

            val children = if (sortMembers) members.sortedBy { it.name.toLowerCase() } else members
            val membersMap = children.groupBy { link(node, it) }



            appendTable("Name", "Summary") {
                appendTableBody {
                    for ((memberLocation, membersList) in membersMap) {
                        val platforms = effectivePlatformsForMembers(membersList)
//                        val platforms = if (platformsBasedOnMembers)
//                            members.flatMapTo(mutableSetOf()) { platformsOfItems(it.members) } + elementPlatforms
//                        else
//                            elementPlatforms

                        val summarized = computeSummarySignatures(membersList)

                        appendIndexRow(platforms) {
                            appendTableCell {
                                if (summarized.platformPlacement == Summarized.PlatformPlacement.Row) {
                                    appendPlatforms(platforms)
                                }
//                                appendHeader(level = 4) {
//                                appendParagraph {
                                appendLink(memberLocation)

                                if (node.sinceKotlin != null) {
                                    appendSinceKotlin(node.sinceKotlin.toString())
                                }

                                if (membersList.singleOrNull()?.sinceKotlin != null){
                                    wrap(" (", ")"){ appendSinceKotlin(membersList.single().sinceKotlin.toString()) }
                                }
//                                  }
//                                    if (members.singleOrNull()?.kind != NodeKind.ExternalClass) {
//                                        appendPlatforms(platforms)
//                                    }
//                                }
                            }
                            appendTableCell {
                                appendSummarySignatures(summarized)
                            }
                        }
                    }
                }
            }
        }

//
//        private fun platformsOfItems(items: List<DocumentationNode>, omitSamePlatforms: Boolean = true): Set<String> {
//            if (items.all { it.kind != NodeKind.Package && it.kind != NodeKind.Module && it.kind != NodeKind.ExternalClass }) {
//                return unionPlatformsOfItems(items)
//            }
//
//            val platforms = platformsOfItems(items)
//            if (platforms.isNotEmpty() && (platforms != node.platformsToShow.toSet() || !omitSamePlatforms)) {
//                return platforms
//            }
//            return emptySet()
//        }



        private fun computeSummarySignatures(items: List<DocumentationNode>): Summarized =
            Summarized(items.groupBy { it.summary }.mapValues { (_, nodes) ->
                val nodesToAppend = nodes.flatMap { if(it.kind == NodeKind.GroupNode) it.origins else listOf(it) }

                val summarySignature = languageService.summarizeSignatures(nodesToAppend)
                if (summarySignature != null) {
                    mapOf(summarySignature to nodesToAppend)
                } else {
                    nodesToAppend.groupBy {
                        languageService.render(it, RenderMode.SUMMARY)
                    }
                }
            })


        private fun appendSummarySignatures(
            summarized: Summarized
        ) {
            for(summary in summarized.data) {

                appendAsSummaryGroup(summary.platforms) {
                    if (summarized.platformPlacement == Summarized.PlatformPlacement.Summary) {
                        appendPlatforms(summary.platforms)
                    }
                    summary.signatures.subList(0, summary.signatures.size - 1).forEach {
                        appendSignatures(
                            it,
                            summarized.platformPlacement == Summarized.PlatformPlacement.Signature
                        )
                        appendLine()
                    }
                    appendSignatures(
                        summary.signatures.last(),
                        summarized.platformPlacement == Summarized.PlatformPlacement.Signature
                    )
                    appendContent(summary.content)
                }

            }
        }

        private fun appendSignatures(
            signature: Summarized.SummarizedNodes,
            withPlatforms: Boolean
        ) {

//            val platforms = if (platformsBasedOnMembers)
//                items.flatMapTo(mutableSetOf()) { platformsOfItems(it.members) } + elementPlatforms
//            else
//                elementPlatforms


            appendAsPlatformDependentBlock(signature.platforms) {
                if (withPlatforms) {
                    appendPlatforms(signature.platforms)
                }
                appendAsSignature(signature.content) {
                    signature.content.appendSignature()
                }
                appendSoftLineBreak()
            }
        }
    }

    private fun DocumentationNode.isClassLikeGroupNode(): Boolean {
        if (kind != NodeKind.GroupNode) {
            return false
        }

        return origins.all { it.kind in NodeKind.classLike }
    }


    inner class AllTypesNodeBuilder(val node: DocumentationNode)
        : PageBuilder(listOf(node)) {

        override fun build() {
            appendContent(node.owner!!.summary)
            appendHeader(3) { to.append("All Types") }

            appendTable("Name", "Summary") {
                appendTableBody {
                    for (type in node.members) {
                        val platforms = effectivePlatformsForNode(type)
                        appendIndexRow(platforms) {
                            appendPlatforms(platforms)
                            appendHeader(level = 5) {
                                appendLink(link(node, type) {
                                    if (it.kind == NodeKind.ExternalClass) it.name else it.qualifiedName()
                                })
                            }

                            if (type.kind == NodeKind.ExternalClass) {
                                val packageName = type.owner?.name
                                if (packageName != null) {
                                    appendText(" (extensions in package $packageName)")
                                }
                            }

                            appendContent(type.summary)
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

abstract class StructuredFormatService(val generator: NodeLocationAwareGenerator,
                                       val languageService: LanguageService,
                                       override val extension: String,
                                       override final val linkExtension: String = extension) : FormatService {

}

typealias PlatformsData = Map<String, Set<DocumentationNode>>

fun memberPlatforms(node: DocumentationNode): PlatformsData {
    val members = when {
        node.kind == NodeKind.GroupNode -> node.origins
        node.kind in NodeKind.classLike -> emptyList()
        node.kind in NodeKind.memberLike -> emptyList()
        else -> node.members
    }

    return members.map(::effectivePlatformsForNode).fold(mapOf(), ::mergePlatforms)
}

fun mergePlatforms(a: PlatformsData, b: PlatformsData): PlatformsData {
    val mutable = a.toMutableMap()
    b.forEach { (name, declarations) ->
        mutable.merge(name, declarations) { a, b -> a.union(b) }
    }
    return mutable
}

fun effectivePlatformsForNode(node: DocumentationNode): PlatformsData {
    val platforms = node.platforms + memberPlatforms(node).keys
    return platforms.keysToMap { setOf(node) }
}

fun effectivePlatformsForMembers(nodes: Collection<DocumentationNode>): PlatformsData {
    return nodes.map { effectivePlatformsForNode(it) }.reduce(::mergePlatforms)
}

fun mergeVersions(kotlinVersions: List<String>): String {
    return kotlinVersions.distinct().min()!!
}

fun effectiveSinceKotlinForNode(node: DocumentationNode, baseVersion: String = "1.0"): String {
    val members = when {
        node.kind == NodeKind.GroupNode -> node.origins
        node.kind in NodeKind.classLike -> emptyList()
        node.kind in NodeKind.memberLike -> emptyList()
        else -> node.members
    }
    val newBase = node.sinceKotlin ?: baseVersion
    val memberVersion = if (members.isNotEmpty()) effectiveSinceKotlinForNodes(members, newBase) else newBase

    return node.sinceKotlin ?: memberVersion
}

fun effectiveSinceKotlinForNodes(nodes: Collection<DocumentationNode>, baseVersion: String = "1.0"): String {
    val map = nodes.map { effectiveSinceKotlinForNode(it, baseVersion) }
    return mergeVersions(map)
}

fun samePlatforms(platformsPerNode: Collection<PlatformsData>): Boolean {

    val first = platformsPerNode.firstOrNull()?.keys ?: return true
    return platformsPerNode.all { it.keys == first }
}

fun locationHref(
    from: Location,
    to: DocumentationNode,
    generator: NodeLocationAwareGenerator,
    pathOnly: Boolean = false
): String {
    val topLevelPage = to.references(RefKind.TopLevelPage).singleOrNull()?.to
    if (topLevelPage != null) {
        val signature = to.detailOrNull(NodeKind.Signature)
        return from.relativePathTo(
            generator.location(topLevelPage),
            (signature?.name ?: to.name).takeUnless { pathOnly }
        )
    }
    return from.relativePathTo(generator.location(to))
}