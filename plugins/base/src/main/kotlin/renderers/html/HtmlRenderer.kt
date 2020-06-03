package org.jetbrains.dokka.base.renderers.html

import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import java.io.File

open class HtmlRenderer(
    context: DokkaContext
) : DefaultRenderer<FlowContent>(context) {

    private val sourceSetDependencyMap = with(context.sourceSetCache) {
        allSourceSets.map { sourceSet ->
            sourceSet to allSourceSets.filter { sourceSet.dependentSourceSets.contains(it.sourceSetName ) }
        }.toMap()
    }

    private val pageList = mutableListOf<String>()

    override val preprocessors = context.plugin<DokkaBase>().query { htmlPreprocessors } +
            context.plugin<DokkaBase>().querySingle { samplesTransformer }

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        val additionalClasses = node.style.joinToString(" ") { it.toString().toLowerCase() }
        return when {
            node.hasStyle(ContentStyle.TabbedContent) -> div(additionalClasses) {
                val secondLevel = node.children.filterIsInstance<ContentComposite>().flatMap { it.children }.filterIsInstance<ContentHeader>().flatMap { it.children }.filterIsInstance<ContentText>()
                val firstLevel = node.children.filterIsInstance<ContentHeader>().flatMap { it.children }.filterIsInstance<ContentText>()

                val renderable = firstLevel.union(secondLevel)

                div(classes = "tabs-section"){
                    attributes["tabs-section"] = "tabs-section"
                    renderable.forEachIndexed { index, node ->
                        button(classes = "section-tab"){
                            if(index == 0 ) attributes["data-active"] = ""
                            attributes["data-togglable"] = node.text
                            text(node.text)
                        }
                    }
                }
                div(classes = "tabs-section-body"){
                    childrenCallback()
                }
            }
            node.hasStyle(ContentStyle.WithExtraAttributes) -> div() {
                node.extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
                childrenCallback()
            }
            node.hasStyle(TextStyle.BreakableAfter) -> {
                span(){ childrenCallback() }
                wbr {  }
            }
            node.hasStyle(TextStyle.Breakable) -> {
                span("breakable-word"){ childrenCallback() }
            }
            node.hasStyle(TextStyle.Span) -> span(){ childrenCallback() }
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
                    attributes["data-filter"] = it.sourceSetName
                    when(it.platform.key){
                        "common" -> classes = classes + "common-like"
                        "native" -> classes = classes + "native-like"
                        "jvm" -> classes = classes + "jvm-like"
                        "js" -> classes = classes + "js-like"
                    }
                    text(it.sourceSetName)
                }
            }
        }
    }

    override fun FlowContent.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) =
        buildPlatformDependent(content.sourceSets.map { it to setOf(content.inner) }.toMap(), pageContext, content.extra)

    private fun FlowContent.buildPlatformDependent(
        nodes: Map<SourceSetData, Collection<ContentNode>>,
        pageContext: ContentPage,
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
    ) {
        var mergedToOneSourceSet : SourceSetData? = null
        div("platform-hinted") {
            attributes["data-platform-hinted"] = "data-platform-hinted"
            extra.extraHtmlAttributes().forEach { attributes[it.extraKey] = it.extraValue }
            val additionalClasses = if(nodes.toList().size == 1) "single-content" else ""
            var counter = 0
            val contents = nodes.toList().map { (sourceSet, elements) ->
                sourceSet to createHTML(prettyPrint = false).div {
                    elements.forEach {
                        buildContentNode(it, pageContext, setOf(sourceSet))
                    }
                }.stripDiv()
            }.groupBy(Pair<SourceSetData, String>::second, Pair<SourceSetData, String>::first).entries.flatMap { (html, sourceSets) ->
                sourceSets.filterNot {
                    sourceSetDependencyMap[it].orEmpty().any { dependency -> sourceSets.contains(dependency) }
                }.map {
                    it to createHTML(prettyPrint = false).div(classes = "content $additionalClasses") {
                        if (counter++ == 0) attributes["data-active"] = ""
                        attributes["data-togglable"] = it.sourceSetName
                        unsafe {
                            +html
                        }
                    }
                }
            }
            if (contents.size != 1) {
                div("platform-bookmarks-row") {
                    attributes["data-toggle-list"] = "data-toggle-list"
                    contents.forEachIndexed { index, pair ->
                        button(classes = "platform-bookmark") {
                            attributes["data-filterable-current"] = pair.first.sourceSetName
                            attributes["data-filterable-set"] = pair.first.sourceSetName
                            if (index == 0) attributes["data-active"] = ""
                            attributes["data-toggle"] = pair.first.sourceSetName
                            when(
                                pair.first.platform.key
                            ){
                            "common" -> classes = classes + "common-like"
                            "native" -> classes = classes + "native-like"
                            "jvm" -> classes = classes + "jvm-like"
                            "js" -> classes = classes + "js-like"
                            }
                            attributes["data-toggle"] = pair.first.sourceSetName
                            text(pair.first.sourceSetName)
                        }
                    }
                }
            } else if (nodes.size > 1) {
                mergedToOneSourceSet = contents.first().first
            }
            contents.forEach {
                consumer.onTagContentUnsafe { +it.second }
            }
        }
        mergedToOneSourceSet?.let { createPlatformTagBubbles(listOf(it)) }
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
                Pair<Pair<ContentDivergentInstance, SourceSetData>, Pair<String, String>>::second,
                Pair<Pair<ContentDivergentInstance, SourceSetData>, Pair<String, String>>::first
            )

        distinct.forEach {
            val groupedDivergent = it.value.groupBy { it.second }

            consumer.onTagContentUnsafe {
                +createHTML().div("divergent-group"){
                    attributes["data-filterable-current"] = groupedDivergent.keys.joinToString(" ") {
                        it.sourceSetName
                    }
                    attributes["data-filterable-set"] = groupedDivergent.keys.joinToString(" ") {
                        it.sourceSetName
                    }
                    consumer.onTagContentUnsafe { +it.key.first }
                    div("main-subrow") {

                        if (node.implicitlySourceSetHinted) {
                            buildPlatformDependent(
                                groupedDivergent.map { (sourceSet, elements) ->
                                    sourceSet to elements.map { e -> e.first.divergent }
                                }.toMap(),
                                pageContext
                            )
                            if (distinct.size > 1 && groupedDivergent.size == 1) {
                                createPlatformTags(node, groupedDivergent.keys)
                            }
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
        sourceSetRestriction: Set<SourceSetData>?
    ) = if (node.ordered) ol { buildListItems(node.children, pageContext, sourceSetRestriction) }
    else ul { buildListItems(node.children, pageContext, sourceSetRestriction) }

    open fun OL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<SourceSetData>? = null
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
        sourceSetRestriction: Set<SourceSetData>? = null
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
        sourceSetRestriction: Set<SourceSetData>?
    ) {
        node.children
            .filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
            .takeIf { it.isNotEmpty() }
            ?.let {
                withAnchor(node.dci.dri.first().toString()) {
                    div(classes = "table-row") {
                        attributes["data-filterable-current"] = node.sourceSets.joinToString(" ") {
                            it.sourceSetName
                        }
                        attributes["data-filterable-set"] = node.sourceSets.joinToString(" ") {
                            it.sourceSetName
                        }
                        it.filterIsInstance<ContentLink>().takeIf { it.isNotEmpty() }?.let {
                            div("main-subrow " + node.style.joinToString(" ")) {
                                it.filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
                                    .forEach {
                                        it.build(this, pageContext, sourceSetRestriction)
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

    private fun FlowContent.createPlatformTagBubbles(sourceSets: List<SourceSetData>) {
        div("platform-tags") {
            sourceSets.forEach {
                div("platform-tag") {
                    when(it.platform.key){
                        "common" -> classes = classes + "common-like"
                        "native" -> classes = classes + "native-like"
                        "jvm" -> classes = classes + "jvm-like"
                        "js" -> classes = classes + "js-like"
                    }
                    text(it.sourceSetName)
                }
            }
        }
    }

    private fun FlowContent.createPlatformTags(node: ContentNode, sourceSetRestriction: Set<SourceSetData>? = null) {
        node.takeIf { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }?.let {
            createPlatformTagBubbles( node.sourceSets.filter {
                sourceSetRestriction == null || it in sourceSetRestriction
            })
        }
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<SourceSetData>?
    ) {
        when(node.dci.kind){
            ContentKind.Comment -> buildDefaultTable(node, pageContext, sourceSetRestriction)
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
        sourceSetRestriction: Set<SourceSetData>?
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
        when (level) {
            1 -> h1() { withAnchor(anchor, content) }
            2 -> h2() { withAnchor(anchor, content) }
            3 -> h3() { withAnchor(anchor, content) }
            4 -> h4() { withAnchor(anchor, content) }
            5 -> h5() { withAnchor(anchor, content) }
            else -> h6() { withAnchor(anchor, content) }
        }
    }

    private fun FlowContent.withAnchor(anchorName: String?, content: FlowContent.() -> Unit) {
        a {
            anchorName?.let { attributes["name"] = it }
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

    fun FlowContent.buildLink(
        to: DRI,
        platforms: List<SourceSetData>,
        from: PageNode? = null,
        block: FlowContent.() -> Unit
    ) = buildLink(locationProvider.resolve(to, platforms, from), block)

    override fun buildError(node: ContentNode) {
        context.logger.error("Unknown ContentNode type: $node")
    }

    override fun FlowContent.buildNewLine() {
        br()
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) =
        a(href = address, block = content)

    override fun FlowContent.buildCode(
        code: List<ContentNode>,
        language: String,
        pageContext: ContentPage
    ) {
        span(classes = "code") {
            val iterator = code.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                +(when (element) {
                    is ContentText -> element.text
                    is ContentBreakLine -> "\n"
                    else -> run { context.logger.error("Cannot cast $element as ContentText!"); "" }
                })
                if (iterator.hasNext()) {
                    buildNewLine()
                }
            }
        }
    }

    override suspend fun renderPage(page: PageNode) {
        super.renderPage(page)
        if (page is ContentPage) {
            pageList.add(
                """{ "name": "${page.name}", ${if (page is ClasslikePageNode) "\"class\": \"${page.name}\"," else ""} "location": "${locationProvider.resolve(
                    page
                )}" }"""
            )
        }
    }

    override fun FlowContent.buildText(textNode: ContentText) {
        when {
            textNode.hasStyle(TextStyle.Indented) -> consumer.onTagContentEntity(Entities.nbsp)
        }
        text(textNode.text)
    }

    override fun render(root: RootPageNode) {
        super.render(root)
        runBlocking(Dispatchers.Default) {
            launch {
                outputWriter.write("scripts/pages", "var pages = [\n${pageList.joinToString(",\n")}\n]", ".js")
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

    open fun buildHtml(page: PageNode, resources: List<String>, content: FlowContent.() -> Unit) =
        createHTML().html {
            head {
                meta(name = "viewport", content = "width=device-width, initial-scale=1", charset = "UTF-8")
                title(page.name)
                with(resources) {
                    filter { it.substringBefore('?').substringAfterLast('.') == "css" }
                        .forEach { link(rel = LinkRel.stylesheet, href = page.root(it)) }
                    filter { it.substringBefore('?').substringAfterLast('.') == "js" }
                        .forEach { script(type = ScriptType.textJavaScript, src = page.root(it)) { async = true } }
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