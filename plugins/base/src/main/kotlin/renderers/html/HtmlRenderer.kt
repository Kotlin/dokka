package org.jetbrains.dokka.base.renderers.html

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import java.io.File
import java.net.URI

open class HtmlRenderer(
    context: DokkaContext
) : DefaultRenderer<FlowContent>(context) {

    private val sourceSetDependencyMap = context.configuration.sourceSets.map { sourceSet ->
        sourceSet to context.configuration.sourceSets.filter { sourceSet.dependentSourceSets.contains(it.sourceSetID) }
    }.toMap()


    private val pageList = mutableMapOf<String, Pair<String, String>>()

    override val preprocessors = context.plugin<DokkaBase>().query { htmlPreprocessors }

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

                val renderable = firstLevel.union(secondLevel)

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
            node.hasStyle(ContentStyle.WithExtraAttributes) -> div() {
                node.extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
                childrenCallback()
            }
            node.dci.kind in setOf(ContentKind.Symbol, ContentKind.Sample) -> div("symbol $additionalClasses") {
                childrenCallback()
                if (node.hasStyle(TextStyle.Monospace)) copyButton()
            }
            node.hasStyle(TextStyle.BreakableAfter) -> {
                span() { childrenCallback() }
                wbr { }
            }
            node.hasStyle(TextStyle.Breakable) -> {
                span("breakable-word") { childrenCallback() }
            }
            node.hasStyle(TextStyle.Span) -> span() { childrenCallback() }
            node.dci.kind == ContentKind.Symbol -> div("symbol $additionalClasses") { childrenCallback() }
            node.dci.kind == ContentKind.BriefComment -> div("brief $additionalClasses") { childrenCallback() }
            node.dci.kind == ContentKind.Cover -> div("cover $additionalClasses") {
                filterButtons(node)
                childrenCallback()
            }
            node.hasStyle(TextStyle.Paragraph) -> p(additionalClasses) { childrenCallback() }
            node.hasStyle(TextStyle.Block) -> div(additionalClasses) { childrenCallback() }
            else -> childrenCallback()
        }
    }

    private fun FlowContent.filterButtons(group: ContentGroup) {
        div(classes = "filter-section") {
            id = "filter-section"
            group.sourceSets.forEach {
                button(classes = "platform-tag platform-selector") {
                    attributes["data-active"] = ""
                    attributes["data-filter"] = it.sourceSetID.toString()
                    when (it.analysisPlatform.key) {
                        "common" -> classes = classes + "common-like"
                        "native" -> classes = classes + "native-like"
                        "jvm" -> classes = classes + "jvm-like"
                        "js" -> classes = classes + "js-like"
                    }
                    text(it.displayName)
                }
            }
        }
    }

    private fun FlowContent.copyButton() = span(classes = "top-right-position") {
        span("copy-icon") {
            unsafe {
                raw(
                    """<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path fill-rule="evenodd" clip-rule="evenodd" d="M5 4H15V16H5V4ZM17 7H19V18V20H17H8V18H17V7Z" fill="black"/>
                       </svg>""".trimIndent()
                )
            }
        }
        copiedPopup("Content copied to clipboard", "popup-to-left")
    }

    private fun FlowContent.copiedPopup(notificationContent: String, additionalClasses: String = "") =
        div("copy-popup-wrapper $additionalClasses") {
            unsafe {
                raw(
                    """
                    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M18 9C18 14 14 18 9 18C4 18 0 14 0 9C0 4 4 0 9 0C14 0 18 4 18 9ZM14.2 6.2L12.8 4.8L7.5 10.1L5.3 7.8L3.8 9.2L7.5 13L14.2 6.2Z" fill="#4DBB5F"/>
                    </svg>
                    """.trimIndent()
                )
            }
            span {
                text(notificationContent)
            }
        }

    override fun FlowContent.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) =
        buildPlatformDependent(
            content.sourceSets.filter {
                sourceSetRestriction == null || it in sourceSetRestriction
            }.map { it to setOf(content.inner) }.toMap(),
            pageContext,
            content.extra,
            content.style
        )

    private fun FlowContent.buildPlatformDependent(
        nodes: Map<DokkaSourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        styles: Set<Style> = emptySet()
    ) {
        val contents = contentsForSourceSetDependent(nodes, pageContext)
        val shouldHaveTabs = contents.size != 1

        val styles = "platform-hinted ${styles.joinToString()}" + if (shouldHaveTabs) " with-platform-tabs" else ""
        div(styles) {
            attributes["data-platform-hinted"] = "data-platform-hinted"
            extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
            if (shouldHaveTabs) {
                div("platform-bookmarks-row") {
                    attributes["data-toggle-list"] = "data-toggle-list"
                    contents.forEachIndexed { index, pair ->
                        button(classes = "platform-bookmark") {
                            attributes["data-filterable-current"] = pair.first.sourceSetID.toString()
                            attributes["data-filterable-set"] = pair.first.sourceSetID.toString()
                            if (index == 0) attributes["data-active"] = ""
                            attributes["data-toggle"] = pair.first.sourceSetID.toString()
                            when (
                                pair.first.analysisPlatform.key
                                ) {
                                "common" -> classes = classes + "common-like"
                                "native" -> classes = classes + "native-like"
                                "jvm" -> classes = classes + "jvm-like"
                                "js" -> classes = classes + "js-like"
                            }
                            attributes["data-toggle"] = pair.first.sourceSetID.toString()
                            text(pair.first.displayName)
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
        nodes: Map<DokkaSourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
    ): List<Pair<DokkaSourceSet, String>> {
        var counter = 0
        return nodes.toList().map { (sourceSet, elements) ->
            sourceSet to createHTML(prettyPrint = false).div {
                elements.forEach {
                    buildContentNode(it, pageContext, setOf(sourceSet))
                }
            }.stripDiv()
        }.groupBy(
            Pair<DokkaSourceSet, String>::second,
            Pair<DokkaSourceSet, String>::first
        ).entries.flatMap { (html, sourceSets) ->
            sourceSets.filterNot {
                sourceSetDependencyMap[it].orEmpty().any { dependency -> sourceSets.contains(dependency) }
            }.map {
                it to createHTML(prettyPrint = false).div(classes = "content sourceset-depenent-content") {
                    if (counter++ == 0) attributes["data-active"] = ""
                    attributes["data-togglable"] = it.sourceSetID.toString()
                    unsafe {
                        +html
                    }
                }
            }
        }
    }

    override fun FlowContent.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {
        val distinct =
            node.children.flatMap { instance ->
                instance.sourceSets.map { sourceSet ->
                    Pair(instance, sourceSet) to Pair(
                        createHTML(prettyPrint = false).div {
                            instance.before?.let { before ->
                                buildContentNode(before, pageContext, setOf(sourceSet))
                            }
                        }.stripDiv(),
                        createHTML(prettyPrint = false).div {
                            instance.after?.let { after ->
                                buildContentNode(after, pageContext, setOf(sourceSet))
                            }
                        }.stripDiv()
                    )
                }
            }.groupBy(
                Pair<Pair<ContentDivergentInstance, DokkaSourceSet>, Pair<String, String>>::second,
                Pair<Pair<ContentDivergentInstance, DokkaSourceSet>, Pair<String, String>>::first
            )

        distinct.forEach {
            val groupedDivergent = it.value.groupBy { it.second }

            consumer.onTagContentUnsafe {
                +createHTML().div("divergent-group") {
                    attributes["data-filterable-current"] = groupedDivergent.keys.joinToString(" ") {
                        it.sourceSetID.toString()
                    }
                    attributes["data-filterable-set"] = groupedDivergent.keys.joinToString(" ") {
                        it.sourceSetID.toString()
                    }

                    val divergentForPlatformDependent = groupedDivergent.map { (sourceSet, elements) ->
                        sourceSet to elements.map { e -> e.first.divergent }
                    }.toMap()

                    val content = contentsForSourceSetDependent(divergentForPlatformDependent, pageContext)

                    consumer.onTagContentUnsafe {
                        +createHTML().div("brief-with-platform-tags") {
                            consumer.onTagContentUnsafe { +it.key.first }

                            consumer.onTagContentUnsafe {
                                +createHTML().span("pull-right") {
                                    if ((distinct.size > 1 && groupedDivergent.size == 1) || groupedDivergent.size == 1 || content.size == 1) {
                                        if (node.sourceSets.size != 1) {
                                            createPlatformTags(node, setOf(content.first().first))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    div("main-subrow") {
                        if (node.implicitlySourceSetHinted) {
                            buildPlatformDependent(divergentForPlatformDependent, pageContext)
                        } else {
                            it.value.forEach {
                                buildContentNode(it.first.divergent, pageContext, setOf(it.second))
                            }
                        }
                    }
                    consumer.onTagContentUnsafe { +it.key.second }
                }
            }
        }
    }

    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) = if (node.ordered) ol { buildListItems(node.children, pageContext, sourceSetRestriction) }
    else ul { buildListItems(node.children, pageContext, sourceSetRestriction) }

    open fun OL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
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
        sourceSetRestriction: Set<DokkaSourceSet>? = null
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
    ) { // TODO: extension point there
        val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "tif", "webp", "svg")
        return if (File(node.address).extension.toLowerCase() in imageExtensions) {
            //TODO: add imgAttrs parsing
            val imgAttrs = node.extra.allOfType<SimpleAttr>().joinAttr()
            img(src = node.address, alt = node.altText)
        } else {
            println("Unrecognized resource type: $node")
        }
    }

    private fun FlowContent.buildRow(
        node: ContentGroup,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?,
        style: Set<Style>
    ) {
        node.children
            .filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
            .takeIf { it.isNotEmpty() }
            ?.let {
                val anchorName = node.dci.dri.first().toString()
                withAnchor(anchorName) {
                    div(classes = "table-row") {
                        if (!style.contains(MultimoduleTable)) {
                            attributes["data-filterable-current"] = node.sourceSets.joinToString(" ") {
                                it.sourceSetID.toString()
                            }
                            attributes["data-filterable-set"] = node.sourceSets.joinToString(" ") {
                                it.sourceSetID.toString()
                            }
                        }

                        it.filterIsInstance<ContentLink>().takeIf { it.isNotEmpty() }?.let {
                            div("main-subrow " + node.style.joinToString(" ")) {
                                it.filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
                                    .forEach {
                                        span {
                                            it.build(this, pageContext, sourceSetRestriction)
                                            buildAnchor(anchorName)
                                        }
                                        if (ContentKind.shouldBePlatformTagged(node.dci.kind) && (node.sourceSets.size == 1))
                                            createPlatformTags(node)
                                    }
                            }
                        }

                        it.filter { it !is ContentLink }.takeIf { it.isNotEmpty() }?.let {
                            div("platform-dependent-row keyValue") {
                                val title = it.filter { it.style.contains(ContentStyle.RowTitle) }
                                div {
                                    title.forEach {
                                        it.build(this, pageContext, sourceSetRestriction)
                                    }
                                }
                                div("title") {
                                    (it - title).forEach {
                                        it.build(this, pageContext, sourceSetRestriction)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun FlowContent.createPlatformTagBubbles(sourceSets: List<DokkaSourceSet>) {
        div("platform-tags") {
            sourceSets.forEach {
                div("platform-tag") {
                    when (it.analysisPlatform.key) {
                        "common" -> classes = classes + "common-like"
                        "native" -> classes = classes + "native-like"
                        "jvm" -> classes = classes + "jvm-like"
                        "js" -> classes = classes + "js-like"
                    }
                    text(it.displayName)
                }
            }
        }
    }

    private fun FlowContent.createPlatformTags(node: ContentNode, sourceSetRestriction: Set<DokkaSourceSet>? = null) {
        node.takeIf { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }?.let {
            createPlatformTagBubbles(node.sourceSets.filter {
                sourceSetRestriction == null || it in sourceSetRestriction
            })
        }
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) {
        when (node.dci.kind) {
            ContentKind.Comment -> buildDefaultTable(node, pageContext, sourceSetRestriction)
            else -> div(classes = "table") {
                node.extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
                node.children.forEach {
                    buildRow(it, pageContext, sourceSetRestriction, node.style)
                }
            }
        }

    }

    fun FlowContent.buildDefaultTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
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
        val anchor = node.extra[SimpleAttr.SimpleAttrKey("anchor")]?.extraValue
        val classes = node.style.joinToString { it.toString() }.toLowerCase()
        when (level) {
            1 -> h1(classes = classes) { withAnchor(anchor, content) }
            2 -> h2(classes = classes) { withAnchor(anchor, content) }
            3 -> h3(classes = classes) { withAnchor(anchor, content) }
            4 -> h4(classes = classes) { withAnchor(anchor, content) }
            5 -> h5(classes = classes) { withAnchor(anchor, content) }
            else -> h6(classes = classes) { withAnchor(anchor, content) }
        }
    }

    private fun FlowContent.withAnchor(anchorName: String?, content: FlowContent.() -> Unit) {
        a {
            anchorName?.let { attributes["data-name"] = it }
        }
        content()
    }


    override fun FlowContent.buildNavigation(page: PageNode) =
        div(classes = "breadcrumbs") {
            locationProvider.ancestors(page).asReversed().forEach { node ->
                text("/")
                if (node.isNavigable) buildLink(node, page)
                else text(node.name)
            }
        }

    private fun FlowContent.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)) {
            text(to.name)
        }

    private fun FlowContent.buildAnchor(pointingTo: String) {
        span(classes = "anchor-wrapper") {
            span(classes = "anchor-icon") {
                attributes["pointing-to"] = pointingTo
                unsafe {
                    raw(
                        """
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M21.2496 5.3C20.3496 4.5 19.2496 4 18.0496 4C16.8496 4 15.6496 4.5 14.8496 5.3L10.3496 9.8L11.7496 11.2L16.2496 6.7C17.2496 5.7 18.8496 5.7 19.8496 6.7C20.8496 7.7 20.8496 9.3 19.8496 10.3L15.3496 14.8L16.7496 16.2L21.2496 11.7C22.1496 10.8 22.5496 9.7 22.5496 8.5C22.5496 7.3 22.1496 6.2 21.2496 5.3Z"/>
                    <path d="M8.35 16.7998C7.35 17.7998 5.75 17.7998 4.75 16.7998C3.75 15.7998 3.75 14.1998 4.75 13.1998L9.25 8.6998L7.85 7.2998L3.35 11.7998C1.55 13.5998 1.55 16.3998 3.35 18.1998C4.25 19.0998 5.35 19.4998 6.55 19.4998C7.75 19.4998 8.85 19.0998 9.75 18.1998L14.25 13.6998L12.85 12.2998L8.35 16.7998Z"/>
                </svg>
            """.trimIndent()
                    )
                }
            }
            copiedPopup("Link copied to clipboard")
        }
    }

    fun FlowContent.buildLink(
        to: DRI,
        platforms: List<DokkaSourceSet>,
        from: PageNode? = null,
        block: FlowContent.() -> Unit
    ) = buildLink(locationProvider.resolve(to, platforms.toSet(), from), block)

    override fun buildError(node: ContentNode) {
        context.logger.error("Unknown ContentNode type: $node")
    }

    override fun FlowContent.buildNewLine() {
        br()
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) =
        a(href = address, block = content)

    override fun FlowContent.buildCode(
        code: ContentCode,
        pageContext: ContentPage
    ) {
        code(code.style.joinToString(" ") { it.toString().toLowerCase() }) {
            code.children.forEach { buildContentNode(it, pageContext) }
        }
    }

    private fun getSymbolSignature(page: ContentPage) = page.content.dfs { it.dci.kind == ContentKind.Symbol }

    private fun flattenToText(node: ContentNode): String {
        fun getContentTextNodes(node: ContentNode, sourceSetRestriction: DokkaSourceSet): List<ContentText> =
            when (node) {
                is ContentText -> listOf(node)
                is ContentComposite -> node.children
                    .filter { sourceSetRestriction in it.sourceSets }
                    .flatMap { getContentTextNodes(it, sourceSetRestriction) }
                    .takeIf { node.dci.kind != ContentKind.Annotations }
                    .orEmpty()
                else -> emptyList()
            }

        val sourceSetRestriction =
            node.sourceSets.find { it.analysisPlatform == Platform.common } ?: node.sourceSets.first()
        return getContentTextNodes(node, sourceSetRestriction).joinToString("") { it.text }
    }

    override suspend fun renderPage(page: PageNode) {
        super.renderPage(page)
        if (page is ContentPage && page !is ModulePageNode && page !is PackagePageNode) {
            val signature = getSymbolSignature(page)
            val textNodes = signature?.let { flattenToText(it) }
            val documentable = page.documentable
            if (documentable != null) {
                listOf(
                    documentable.dri.packageName,
                    documentable.dri.classNames,
                    documentable.dri.callable?.name
                )
                    .filter { !it.isNullOrEmpty() }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(".")
                    ?.let {
                        pageList.put(it, Pair(textNodes ?: page.name, locationProvider.resolve(page)))
                    }

            }
        }
    }

    override fun FlowContent.buildText(textNode: ContentText) {
        when {
            textNode.hasStyle(TextStyle.Indented) -> consumer.onTagContentEntity(Entities.nbsp)
        }
        text(textNode.text)
    }

    private fun generatePagesList() =
        pageList.entries
            .filter { !it.key.isNullOrEmpty() }
            .groupBy { it.key.substringAfterLast(".") }
            .entries
            .mapIndexed { topLevelIndex, entry ->
                if (entry.value.size > 1) {
                    listOf(
                        "{\'name\': \'${entry.key}\', \'index\': \'$topLevelIndex\', \'disabled\': true}"
                    ) + entry.value.mapIndexed { index, subentry ->
                        "{\'name\': \'${subentry.value.first}\', \'level\': 1, \'index\': \'$topLevelIndex.$index\', \'description\':\'${subentry.key}\', \'location\':\'${subentry.value.second}\'}"
                    }
                } else {
                    val subentry = entry.value.single()
                    listOf(
                        "{\'name\': \'${subentry.value.first}\', \'index\': \'$topLevelIndex\', \'description\':\'${subentry.key}\', \'location\':\'${subentry.value.second}\'}"
                    )
                }
            }
            .flatten()
            .joinToString(prefix = "[", separator = ",\n", postfix = "]")

    override fun render(root: RootPageNode) {
        super.render(root)
        runBlocking(Dispatchers.Default) {
            launch {
                outputWriter.write("scripts/pages", "var pages = ${generatePagesList()}", ".js")
            }
        }
    }

    private fun PageNode.root(path: String) = locationProvider.resolveRoot(this) + path

    override fun buildPage(page: ContentPage, content: (FlowContent, ContentPage) -> Unit): String =
        buildHtml(page, page.embeddedResources) {
            div {
                id = "content"
                attributes["pageIds"] = page.dri.first().toString()
                content(this, page)
            }
        }

    private fun resolveLink(link: String, page: PageNode): String = if (URI(link).isAbsolute) link else page.root(link)

    open fun buildHtml(page: PageNode, resources: List<String>, content: FlowContent.() -> Unit) =
        createHTML().html {
            head {
                meta(name = "viewport", content = "width=device-width, initial-scale=1", charset = "UTF-8")
                title(page.name)
                resources.forEach {
                    when {
                        it.substringBefore('?').substringAfterLast('.') == "css" -> link(
                            rel = LinkRel.stylesheet,
                            href = resolveLink(it, page)
                        )
                        it.substringBefore('?').substringAfterLast('.') == "js" -> script(
                            type = ScriptType.textJavaScript,
                            src = resolveLink(it, page)
                        ) {
                            async = true
                        }
                        else -> unsafe { +it }
                    }
                }
                script { unsafe { +"""var pathToRoot = "${locationProvider.resolveRoot(page)}";""" } }
            }
            body {
                div {
                    id = "container"
                    div {
                        id = "leftColumn"
                        div {
                            id = "logo"
                        }
                        div {
                            id = "sideMenu"
                        }
                    }
                    div {
                        id = "main"
                        div {
                            id = "searchBar"
                        }
                        script(type = ScriptType.textJavaScript, src = page.root("scripts/pages.js")) {}
                        script(type = ScriptType.textJavaScript, src = page.root("scripts/main.js")) {}
                        content()
                        div(classes = "footer") {
                            span("go-to-top-icon") {
                                a(href = "#container") {
                                    unsafe {
                                        raw(
                                            """
                                    <svg width="12" height="10" viewBox="0 0 12 10" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M11.3337 9.66683H0.666992L6.00033 3.66683L11.3337 9.66683Z" fill="black"/>
                                        <path d="M0.666992 0.333496H11.3337V1.66683H0.666992V0.333496Z" fill="black"/>
                                    </svg>
                                """.trimIndent()
                                        )
                                    }
                                }
                            }
                            span { text("© 2020 Copyright") }
                            span { text("Privacy Policy") }
                            span("pull-right") {
                                span { text("Sponsored and developed by Dokka") }
                                span(classes = "padded-icon") {
                                    unsafe {
                                        raw(
                                            """
                                    <svg width="8" height="8" viewBox="0 0 8 8" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M8 0H2.3949L4.84076 2.44586L0 7.28662L0.713376 8L5.55414 3.15924L8 5.6051V0Z" fill="black"/>
                                    </svg>
                                """.trimIndent()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

fun List<SimpleAttr>.joinAttr() = joinToString(" ") { it.extraKey + "=" + it.extraValue }

private fun String.stripDiv() = drop(5).dropLast(6) // TODO: Find a way to do it without arbitrary trims

private val PageNode.isNavigable: Boolean
    get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

fun PropertyContainer<ContentNode>.extraHtmlAttributes() = allOfType<SimpleAttr>()
