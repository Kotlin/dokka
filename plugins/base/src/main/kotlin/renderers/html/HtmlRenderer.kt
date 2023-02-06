package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.command.consumers.ImmediateResolutionTagConsumer
import org.jetbrains.dokka.base.renderers.html.innerTemplating.DefaultTemplateModelFactory
import org.jetbrains.dokka.base.renderers.html.innerTemplating.DefaultTemplateModelMerger
import org.jetbrains.dokka.base.renderers.html.innerTemplating.DokkaTemplateTypes
import org.jetbrains.dokka.base.renderers.html.innerTemplating.HtmlTemplater
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.resolvers.local.DokkaBaseLocationProvider
import org.jetbrains.dokka.base.templating.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.pages.HtmlContent
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.utilities.htmlEscape
import org.jetbrains.kotlin.utils.addIfNotNull

internal const val TEMPLATE_REPLACEMENT: String = "###"

open class HtmlRenderer(
    context: DokkaContext
) : DefaultRenderer<FlowContent>(context) {
    private val sourceSetDependencyMap: Map<DokkaSourceSetID, List<DokkaSourceSetID>> =
        context.configuration.sourceSets.associate { sourceSet ->
            sourceSet.sourceSetID to context.configuration.sourceSets
                .map { it.sourceSetID }
                .filter { it in sourceSet.dependentSourceSets }
        }

    private val templateModelFactories = listOf(DefaultTemplateModelFactory(context)) // TODO: Make extension point
    private val templateModelMerger = DefaultTemplateModelMerger()
    private val templater = HtmlTemplater(context).apply {
        setupSharedModel(templateModelMerger.invoke(templateModelFactories) { buildSharedModel() })
    }

    private var shouldRenderSourceSetTabs: Boolean = false

    override val preprocessors = context.plugin<DokkaBase>().query { htmlPreprocessors }

    private val tabSortingStrategy = context.plugin<DokkaBase>().querySingle { tabSortingStrategy }

    private fun <R> TagConsumer<R>.prepareForTemplates() =
        if (context.configuration.delayTemplateSubstitution || this is ImmediateResolutionTagConsumer) this
        else ImmediateResolutionTagConsumer(this, context)

    private fun <T : ContentNode> sortTabs(strategy: TabSortingStrategy, tabs: Collection<T>): List<T> {
        val sorted = strategy.sort(tabs)
        if (sorted.size != tabs.size)
            context.logger.warn("Tab sorting strategy has changed number of tabs from ${tabs.size} to ${sorted.size}")
        return sorted
    }

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        val additionalClasses = node.style.joinToString(" ") { it.toString().toLowerCase() }
        return when {
            node.hasStyle(ContentStyle.TabbedContent) -> div(additionalClasses) {
                val secondLevel = node.children.filterIsInstance<ContentComposite>().flatMap { it.children }
                    .filterIsInstance<ContentHeader>().flatMap { it.children }.filterIsInstance<ContentText>()
                val firstLevel = node.children.filterIsInstance<ContentHeader>().flatMap { it.children }
                    .filterIsInstance<ContentText>()

                val renderable = sortTabs(tabSortingStrategy, firstLevel.union(secondLevel))

                div(classes = "tabs-section") {
                    attributes["tabs-section"] = "tabs-section"
                    renderable.forEachIndexed { index, node ->
                        button(classes = "section-tab") {
                            if (index == 0) attributes["data-active"] = ""
                            attributes["data-togglable"] = node.text
                            text(node.text)
                        }
                    }
                }
                div(classes = "tabs-section-body") {
                    childrenCallback()
                }
            }
            node.hasStyle(ContentStyle.WithExtraAttributes) -> div {
                node.extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
                childrenCallback()
            }
            node.dci.kind in setOf(ContentKind.Symbol) -> div("symbol $additionalClasses") {
                childrenCallback()
            }
            node.hasStyle(ContentStyle.KDocTag) -> span("kdoc-tag") { childrenCallback() }
            node.hasStyle(ContentStyle.Footnote) -> div("footnote") { childrenCallback() }
            node.hasStyle(TextStyle.BreakableAfter) -> {
                span { childrenCallback() }
                wbr { }
            }
            node.hasStyle(TextStyle.Breakable) -> {
                span("breakable-word") { childrenCallback() }
            }
            node.hasStyle(TextStyle.Span) -> span { childrenCallback() }
            node.dci.kind == ContentKind.Symbol -> div("symbol $additionalClasses") { childrenCallback() }
            node.dci.kind == SymbolContentKind.Parameters -> {
                span("parameters $additionalClasses") {
                    childrenCallback()
                }
            }
            node.dci.kind == SymbolContentKind.Parameter -> {
                span("parameter $additionalClasses") {
                    childrenCallback()
                }
            }
            node.hasStyle(TextStyle.InlineComment) -> div("inline-comment") { childrenCallback() }
            node.dci.kind == ContentKind.BriefComment -> div("brief $additionalClasses") { childrenCallback() }
            node.dci.kind == ContentKind.Cover -> div("cover $additionalClasses") { //TODO this can be removed
                childrenCallback()
            }
            node.dci.kind == ContentKind.Deprecation -> div("deprecation-content") { childrenCallback() }
            node.hasStyle(TextStyle.Paragraph) -> p(additionalClasses) { childrenCallback() }
            node.hasStyle(TextStyle.Block) -> div(additionalClasses) { childrenCallback() }
            node.hasStyle(TextStyle.Quotation) -> blockQuote(additionalClasses) { childrenCallback() }
            node.hasStyle(TextStyle.FloatingRight) -> span("clearfix") { span("floating-right") { childrenCallback() } }
            node.hasStyle(TextStyle.Strikethrough) -> strike { childrenCallback() }
            node.isAnchorable -> buildAnchor(
                node.anchor!!,
                node.anchorLabel!!,
                node.sourceSetsFilters
            ) { childrenCallback() }
            node.extra[InsertTemplateExtra] != null -> node.extra[InsertTemplateExtra]?.let { templateCommand(it.command) }
                ?: Unit
            node.hasStyle(ListStyle.DescriptionTerm) -> DT(emptyMap(), consumer).visit {
                this@wrapGroup.childrenCallback()
            }
            node.hasStyle(ListStyle.DescriptionDetails) -> DD(emptyMap(), consumer).visit {
                this@wrapGroup.childrenCallback()
            }
            else -> childrenCallback()
        }
    }

    private fun FlowContent.copyButton() = span(classes = "top-right-position") {
        span("copy-icon")
        copiedPopup("Content copied to clipboard", "popup-to-left")
    }

    private fun FlowContent.copiedPopup(notificationContent: String, additionalClasses: String = "") =
        div("copy-popup-wrapper $additionalClasses") {
            span("copy-popup-icon")
            span {
                text(notificationContent)
            }
        }

    override fun FlowContent.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) =
        buildPlatformDependent(
            content.sourceSets.filter {
                sourceSetRestriction == null || it in sourceSetRestriction
            }.associateWith { setOf(content.inner) },
            pageContext,
            content.extra,
            content.style
        )

    private fun FlowContent.buildPlatformDependent(
        nodes: Map<DisplaySourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        styles: Set<Style> = emptySet(),
        shouldHaveTabs: Boolean = shouldRenderSourceSetTabs
    ) {
        val contents = contentsForSourceSetDependent(nodes, pageContext)
        val isOnlyCommonContent = contents.singleOrNull()?.let { (sourceSet, _) ->
            sourceSet.platform == Platform.common
                    && sourceSet.name.equals("common", ignoreCase = true)
                    && sourceSet.sourceSetIDs.all.all { sourceSetDependencyMap[it]?.isEmpty() == true }
        } ?: false

        // little point in rendering a single "common" tab - it can be
        // assumed that code without any tabs is common by default
        val renderTabs = shouldHaveTabs && !isOnlyCommonContent

        val divStyles = "platform-hinted ${styles.joinToString()}" + if (renderTabs) " with-platform-tabs" else ""
        div(divStyles) {
            attributes["data-platform-hinted"] = "data-platform-hinted"
            extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
            if (renderTabs) {
                div("platform-bookmarks-row") {
                    attributes["data-toggle-list"] = "data-toggle-list"
                    contents.forEachIndexed { index, pair ->
                        button(classes = "platform-bookmark") {
                            attributes["data-filterable-current"] = pair.first.sourceSetIDs.merged.toString()
                            attributes["data-filterable-set"] = pair.first.sourceSetIDs.merged.toString()
                            if (index == 0) attributes["data-active"] = ""
                            attributes["data-toggle"] = pair.first.sourceSetIDs.merged.toString()
                            text(pair.first.name)
                        }
                    }
                }
            }
            contents.forEach {
                consumer.onTagContentUnsafe { +it.second }
            }
        }
    }

    private fun contentsForSourceSetDependent(
        nodes: Map<DisplaySourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
    ): List<Pair<DisplaySourceSet, String>> {
        var counter = 0
        return nodes.toList().map { (sourceSet, elements) ->
            val htmlContent = createHTML(prettyPrint = false).prepareForTemplates().div {
                elements.forEach {
                    buildContentNode(it, pageContext, sourceSet.toSet())
                }
            }.stripDiv()
            sourceSet to createHTML(prettyPrint = false).prepareForTemplates()
                .div(classes = "content sourceset-dependent-content") {
                    if (counter++ == 0) attributes["data-active"] = ""
                    attributes["data-togglable"] = sourceSet.sourceSetIDs.merged.toString()
                    unsafe {
                        +htmlContent
                    }
                }
        }.sortedBy { it.first.comparableKey }
    }

    override fun FlowContent.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {
        if (node.implicitlySourceSetHinted) {
            val groupedInstancesBySourceSet = node.children.flatMap { instance ->
                instance.sourceSets.map { sourceSet -> instance to sourceSet }
            }.groupBy(
                Pair<ContentDivergentInstance, DisplaySourceSet>::second,
                Pair<ContentDivergentInstance, DisplaySourceSet>::first
            )

            val nodes = groupedInstancesBySourceSet.mapValues {
                val distinct =
                    groupDivergentInstancesWithSourceSet(it.value, it.key, pageContext,
                        beforeTransformer = { instance, _, sourceSet ->
                            createHTML(prettyPrint = false).prepareForTemplates().div {
                                instance.before?.let { before ->
                                    buildContentNode(before, pageContext, sourceSet)
                                }
                            }.stripDiv()
                        },
                        afterTransformer = { instance, _, sourceSet ->
                            createHTML(prettyPrint = false).prepareForTemplates().div {
                                instance.after?.let { after ->
                                    buildContentNode(after, pageContext, sourceSet)
                                }
                            }.stripDiv()
                        })

                val isPageWithOverloadedMembers = pageContext is MemberPage && pageContext.documentables().size > 1

                val contentOfSourceSet = mutableListOf<ContentNode>()
                distinct.onEachIndexed{ index, (_, distinctInstances) ->
                    contentOfSourceSet.addIfNotNull(distinctInstances.firstOrNull()?.before)
                    contentOfSourceSet.addAll(distinctInstances.map { it.divergent })
                    contentOfSourceSet.addIfNotNull(
                        distinctInstances.firstOrNull()?.after
                            ?: if (index != distinct.size - 1) ContentBreakLine(it.key) else null
                    )

                    // content kind main is important for declarations list to avoid double line breaks
                    if (node.dci.kind == ContentKind.Main && index != distinct.size - 1) {
                        if (isPageWithOverloadedMembers) {
                            // add some spacing and distinction between function/property overloads.
                            // not ideal, but there's no other place to modify overloads page atm
                            contentOfSourceSet.add(ContentBreakLine(it.key, style = setOf(HorizontalBreakLineStyle)))
                        } else {
                            contentOfSourceSet.add(ContentBreakLine(it.key))
                        }
                    }
                }
                contentOfSourceSet
            }
            buildPlatformDependent(nodes, pageContext)
        } else {
            node.children.forEach {
                buildContentNode(it.divergent, pageContext, it.sourceSets)
            }
        }
    }

    private fun groupDivergentInstancesWithSourceSet(
        instances: List<ContentDivergentInstance>,
        sourceSet: DisplaySourceSet,
        pageContext: ContentPage,
        beforeTransformer: (ContentDivergentInstance, ContentPage, DisplaySourceSet) -> String,
        afterTransformer: (ContentDivergentInstance, ContentPage, DisplaySourceSet) -> String
    ): Map<SerializedBeforeAndAfter, List<ContentDivergentInstance>> =
        instances.map { instance ->
            instance to Pair(
                beforeTransformer(instance, pageContext, sourceSet),
                afterTransformer(instance, pageContext, sourceSet)
            )
        }.groupBy(
            Pair<ContentDivergentInstance, SerializedBeforeAndAfter>::second,
            Pair<ContentDivergentInstance, SerializedBeforeAndAfter>::first
        )

    private fun ContentPage.documentables(): List<Documentable> {
        return (this as? WithDocumentables)?.documentables ?: emptyList()
    }

    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = when {
        node.ordered -> {
            ol { buildListItems(node.children, pageContext, sourceSetRestriction) }
        }
        node.hasStyle(ListStyle.DescriptionList) -> {
            dl { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }
        }
        else -> {
            ul { buildListItems(node.children, pageContext, sourceSetRestriction) }
        }
    }

    open fun OL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext, sourceSetRestriction) }
        }
    }

    open fun UL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext) }
        }
    }

    override fun FlowContent.buildResource(
        node: ContentEmbeddedResource,
        pageContext: ContentPage
    ) = // TODO: extension point there
        if (node.isImage()) {
            img(src = node.address, alt = node.altText)
        } else {
            println("Unrecognized resource type: $node")
        }

    private fun FlowContent.buildRow(
        node: ContentGroup,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        node.children
            .filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
            .takeIf { it.isNotEmpty() }
            ?.let {
                when (pageContext) {
                    is MultimoduleRootPage -> buildRowForMultiModule(node, it, pageContext, sourceSetRestriction)
                    is ModulePage -> buildRowForModule(node, it, pageContext, sourceSetRestriction)
                    else -> buildRowForContent(node, it, pageContext, sourceSetRestriction)
                }
            }
    }

    private fun FlowContent.buildRowForMultiModule(
        contextNode: ContentGroup,
        toRender: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildAnchor(contextNode)
        div(classes = "table-row") {
            div("main-subrow " + contextNode.style.joinToString(separator = " ")) {
                buildRowHeaderLink(toRender, pageContext, sourceSetRestriction, contextNode.anchor, "w-100")
                div {
                    buildRowBriefSectionForDocs(toRender, pageContext, sourceSetRestriction)
                }
            }
        }
    }

    private fun FlowContent.buildRowForModule(
        contextNode: ContentGroup,
        toRender: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildAnchor(contextNode)
        div(classes = "table-row") {
            addSourceSetFilteringAttributes(contextNode)
            div {
                div("main-subrow " + contextNode.style.joinToString(separator = " ")) {
                    buildRowHeaderLink(toRender, pageContext, sourceSetRestriction, contextNode.anchor)
                    div("pull-right") {
                        if (ContentKind.shouldBePlatformTagged(contextNode.dci.kind)) {
                            createPlatformTags(contextNode, cssClasses = "no-gutters")
                        }
                    }
                }
                div {
                    buildRowBriefSectionForDocs(toRender, pageContext, sourceSetRestriction)
                }
            }
        }
    }

    private fun FlowContent.buildRowForContent(
        contextNode: ContentGroup,
        toRender: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildAnchor(contextNode)
        div(classes = "table-row") {
            addSourceSetFilteringAttributes(contextNode)
            div("main-subrow keyValue " + contextNode.style.joinToString(separator = " ")) {
                buildRowHeaderLink(toRender, pageContext, sourceSetRestriction, contextNode.anchor)
                div {
                    toRender.filter { it !is ContentLink && !it.hasStyle(ContentStyle.RowTitle) }
                        .takeIf { it.isNotEmpty() }?.let {
                            div("title") {
                                it.forEach {
                                    it.build(this, pageContext, sourceSetRestriction)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun FlowContent.buildRowHeaderLink(
        toRender: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?,
        anchorDestination: String?,
        classes: String = ""
    ) {
        toRender.filter { it is ContentLink || it.hasStyle(ContentStyle.RowTitle) }.takeIf { it.isNotEmpty() }?.let {
            div(classes) {
                it.filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
                    .forEach {
                        span("inline-flex") {
                            div {
                                it.build(this, pageContext, sourceSetRestriction)
                            }
                            if (it is ContentLink && !anchorDestination.isNullOrBlank()) {
                                buildAnchorCopyButton(anchorDestination)
                            }
                        }
                    }
            }
        }
    }

    private fun FlowContent.addSourceSetFilteringAttributes(
        contextNode: ContentGroup,
    ) {
        attributes["data-filterable-current"] = contextNode.sourceSets.joinToString(" ") {
            it.sourceSetIDs.merged.toString()
        }
        attributes["data-filterable-set"] = contextNode.sourceSets.joinToString(" ") {
            it.sourceSetIDs.merged.toString()
        }
    }

    private fun FlowContent.buildRowBriefSectionForDocs(
        toRender: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?,
    ) {
        toRender.filter { it !is ContentLink }.takeIf { it.isNotEmpty() }?.let {
            it.forEach {
                span(classes = if (it.dci.kind == ContentKind.Comment) "brief-comment" else "") {
                    it.build(this, pageContext, sourceSetRestriction)
                }
            }
        }
    }

    private fun FlowContent.createPlatformTagBubbles(sourceSets: List<DisplaySourceSet>, cssClasses: String = "") {
        div("platform-tags $cssClasses") {
            sourceSets.sortedBy { it.name }.forEach {
                div("platform-tag") {
                    when (it.platform.key) {
                        "common" -> classes = classes + "common-like"
                        "native" -> classes = classes + "native-like"
                        "jvm" -> classes = classes + "jvm-like"
                        "js" -> classes = classes + "js-like"
                        "wasm" -> classes = classes + "wasm-like"
                    }
                    text(it.name)
                }
            }
        }
    }

    private fun FlowContent.createPlatformTags(
        node: ContentNode,
        sourceSetRestriction: Set<DisplaySourceSet>? = null,
        cssClasses: String = ""
    ) {
        node.takeIf { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }?.let {
            createPlatformTagBubbles(node.sourceSets.filter {
                sourceSetRestriction == null || it in sourceSetRestriction
            }.sortedBy { it.name }, cssClasses)
        }
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        when {
            node.style.contains(CommentTable) -> buildDefaultTable(node, pageContext, sourceSetRestriction)
            else -> div(classes = "table") {
                node.extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
                node.children.forEach {
                    buildRow(it, pageContext, sourceSetRestriction)
                }
            }
        }

    }

    fun FlowContent.buildDefaultTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        table {
            thead {
                node.header.forEach {
                    tr {
                        it.children.forEach {
                            th {
                                it.build(this@table, pageContext, sourceSetRestriction)
                            }
                        }
                    }
                }
            }
            tbody {
                node.children.forEach {
                    tr {
                        it.children.forEach {
                            td {
                                it.build(this, pageContext, sourceSetRestriction)
                            }
                        }
                    }
                }
            }
        }
    }


    override fun FlowContent.buildHeader(level: Int, node: ContentHeader, content: FlowContent.() -> Unit) {
        val classes = node.style.joinToString { it.toString() }.toLowerCase()
        when (level) {
            1 -> h1(classes = classes, content)
            2 -> h2(classes = classes, content)
            3 -> h3(classes = classes, content)
            4 -> h4(classes = classes, content)
            5 -> h5(classes = classes, content)
            else -> h6(classes = classes, content)
        }
    }

    private fun FlowContent.buildAnchor(
        anchor: String,
        anchorLabel: String,
        sourceSets: String,
        content: FlowContent.() -> Unit
    ) {
        a {
            attributes["data-name"] = anchor
            attributes["anchor-label"] = anchorLabel
            attributes["id"] = anchor
            attributes["data-filterable-set"] = sourceSets
        }
        content()
    }

    private fun FlowContent.buildAnchor(anchor: String, anchorLabel: String, sourceSets: String) =
        buildAnchor(anchor, anchorLabel, sourceSets) {}

    private fun FlowContent.buildAnchor(node: ContentNode) {
        node.anchorLabel?.let { label -> buildAnchor(node.anchor!!, label, node.sourceSetsFilters) }
    }


    override fun FlowContent.buildNavigation(page: PageNode) =
        div(classes = "breadcrumbs") {
            val path = locationProvider.ancestors(page).filterNot { it is RendererSpecificPage }.asReversed()
            if (path.size > 1) {
                buildNavigationElement(path.first(), page)
                path.drop(1).forEach { node ->
                    span(classes = "delimiter") {
                        text("/")
                    }
                    buildNavigationElement(node, page)
                }
            }
        }

    private fun FlowContent.buildNavigationElement(node: PageNode, page: PageNode) =
        if (node.isNavigable) {
            val isCurrentPage = (node == page)
            if (isCurrentPage) {
                span(classes = "current") {
                    text(node.name)
                }
            } else {
                buildLink(node, page)
            }
        } else {
            text(node.name)
        }

    private fun FlowContent.buildLink(to: PageNode, from: PageNode) =
        locationProvider.resolve(to, from)?.let { path ->
            buildLink(path) {
                text(to.name)
            }
        } ?: span {
            attributes["data-unresolved-link"] = to.name.htmlEscape()
            text(to.name)
        }

    fun FlowContent.buildAnchorCopyButton(pointingTo: String) {
        span(classes = "anchor-wrapper") {
            span(classes = "anchor-icon") {
                attributes["pointing-to"] = pointingTo
            }
            copiedPopup("Link copied to clipboard")
        }
    }

    fun FlowContent.buildLink(
        to: DRI,
        platforms: List<DisplaySourceSet>,
        from: PageNode? = null,
        block: FlowContent.() -> Unit
    ) = locationProvider.resolve(to, platforms.toSet(), from)?.let { buildLink(it, block) }
        ?: run { context.logger.error("Cannot resolve path for `$to` from `$from`"); block() }

    override fun buildError(node: ContentNode) = context.logger.error("Unknown ContentNode type: $node")

    override fun FlowContent.buildLineBreak() = br()
    override fun FlowContent.buildLineBreak(node: ContentBreakLine, pageContext: ContentPage) {
        if (node.style.contains(HorizontalBreakLineStyle)) {
            hr()
        } else {
            buildLineBreak()
        }
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) =
        a(href = address, block = content)

    override fun FlowContent.buildDRILink(
        node: ContentDRILink,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = locationProvider.resolve(node.address, node.sourceSets, pageContext)?.let { address ->
        buildLink(address) {
            buildText(node.children, pageContext, sourceSetRestriction)
        }
    } ?: if (isPartial) {
        templateCommand(ResolveLinkCommand(node.address)) {
            buildText(node.children, pageContext, sourceSetRestriction)
        }
    } else {
        span {
            attributes["data-unresolved-link"] = node.address.toString().htmlEscape()
            buildText(node.children, pageContext, sourceSetRestriction)
        }
    }

    override fun FlowContent.buildCodeBlock(
        code: ContentCodeBlock,
        pageContext: ContentPage
    ) {
        div("sample-container") {
            val codeLang = "lang-" + code.language.ifEmpty { "kotlin" }
            val stylesWithBlock = code.style + TextStyle.Block + codeLang
            pre {
                code(stylesWithBlock.joinToString(" ") { it.toString().toLowerCase() }) {
                    attributes["theme"] = "idea"
                    code.children.forEach { buildContentNode(it, pageContext) }
                }
            }
            /*
            Disable copy button on samples as:
             - it is useless
             - it overflows with playground's run button
             */
            if (!code.style.contains(ContentStyle.RunnableSample)) copyButton()
        }
    }

    override fun FlowContent.buildCodeInline(
        code: ContentCodeInline,
        pageContext: ContentPage
    ) {
        val codeLang = "lang-" + code.language.ifEmpty { "kotlin" }
        val stylesWithBlock = code.style + codeLang
        code(stylesWithBlock.joinToString(" ") { it.toString().toLowerCase() }) {
            code.children.forEach { buildContentNode(it, pageContext) }
        }
    }

    override fun FlowContent.buildText(textNode: ContentText) = buildText(textNode, textNode.style)

    private fun FlowContent.buildText(textNode: ContentText, unappliedStyles: Set<Style>) {
        when {
            textNode.extra[HtmlContent] != null -> {
                consumer.onTagContentUnsafe { raw(textNode.text) }
            }
            unappliedStyles.contains(TextStyle.Indented) -> {
                consumer.onTagContentEntity(Entities.nbsp)
                buildText(textNode, unappliedStyles - TextStyle.Indented)
            }
            unappliedStyles.isNotEmpty() -> {
                val styleToApply = unappliedStyles.first()
                applyStyle(styleToApply) {
                    buildText(textNode, unappliedStyles - styleToApply)
                }
            }
            textNode.hasStyle(ContentStyle.RowTitle) || textNode.hasStyle(TextStyle.Cover) ->
                buildBreakableText(textNode.text)
            else -> text(textNode.text)
        }
    }

    private inline fun FlowContent.applyStyle(styleToApply: Style, crossinline body: FlowContent.() -> Unit) {
        when (styleToApply) {
            TextStyle.Bold -> b { body() }
            TextStyle.Italic -> i { body() }
            TextStyle.Strikethrough -> strike { body() }
            TextStyle.Strong -> strong { body() }
            TextStyle.Var -> htmlVar { body() }
            TextStyle.Underlined -> underline { body() }
            is TokenStyle -> span("token ${styleToApply.prismJsClass()}") { body() }
            else -> body()
        }
    }

    private fun TokenStyle.prismJsClass(): String = when(this) {
        // Prism.js parser adds Builtin token instead of Annotation
        // for some reason, so we also add it for consistency and correct coloring
        TokenStyle.Annotation -> "annotation builtin"
        else -> this.toString().toLowerCase()
    }

    override fun render(root: RootPageNode) {
        shouldRenderSourceSetTabs = shouldRenderSourceSetTabs(root)
        super.render(root)
    }

    override fun buildPage(page: ContentPage, content: (FlowContent, ContentPage) -> Unit): String =
        buildHtml(page, page.embeddedResources) {
            content(this, page)
        }


    open fun buildHtml(page: PageNode, resources: List<String>, content: FlowContent.() -> Unit): String =
        templater.renderFromTemplate(DokkaTemplateTypes.BASE) {
            val generatedContent =
                createHTML().div("main-content") {
                    id = "content"
                    (page as? ContentPage)?.let {
                        attributes["pageIds"] = "${context.configuration.moduleName}::${page.pageId}"
                    }
                    content()
                }

            templateModelMerger.invoke(templateModelFactories) {
                buildModel(
                    page,
                    resources,
                    locationProvider,
                    generatedContent
                )
            }
        }

    /**
     * This is deliberately left open for plugins that have some other pages above ours and would like to link to them
     * instead of ours when clicking the logo
     */
    open fun FlowContent.clickableLogo(page: PageNode, pathToRoot: String) {
        if (context.configuration.delayTemplateSubstitution && page is ContentPage) {
            templateCommand(PathToRootSubstitutionCommand(pattern = "###", default = pathToRoot)) {
                a {
                    href = "###index.html"
                    templateCommand(
                        ProjectNameSubstitutionCommand(
                            pattern = "@@@",
                            default = context.configuration.moduleName
                        )
                    ) {
                        span {
                            text("@@@")
                        }
                    }
                }
            }
        } else {
            a {
                href = pathToRoot + "index.html"
                text(context.configuration.moduleName)
            }
        }
    }

    private val ContentNode.isAnchorable: Boolean
        get() = anchorLabel != null

    private val ContentNode.anchorLabel: String?
        get() = extra[SymbolAnchorHint]?.anchorName

    private val ContentNode.anchor: String?
        get() = extra[SymbolAnchorHint]?.contentKind?.let { contentKind ->
            (locationProvider as DokkaBaseLocationProvider).anchorForDCI(DCI(dci.dri, contentKind), sourceSets)
        }

    private val isPartial = context.configuration.delayTemplateSubstitution
}

fun List<SimpleAttr>.joinAttr() = joinToString(" ") { it.extraKey + "=" + it.extraValue }

private fun String.stripDiv() = drop(5).dropLast(6) // TODO: Find a way to do it without arbitrary trims

private val PageNode.isNavigable: Boolean
    get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

private fun PropertyContainer<ContentNode>.extraHtmlAttributes() = allOfType<SimpleAttr>()

private val ContentNode.sourceSetsFilters: String
    get() = sourceSets.sourceSetIDs.joinToString(" ") { it.toString() }

private val DisplaySourceSet.comparableKey
        get() = sourceSetIDs.merged.let { it.scopeId + it.sourceSetName }
