package org.jetbrains.dokka.base.renderers.html

import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import java.io.File

open class HtmlRenderer(
    context: DokkaContext
) : DefaultRenderer<FlowContent>(context) {

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
            node.dci.kind == ContentKind.Symbol -> div("symbol $additionalClasses") { childrenCallback() }
            node.dci.kind == ContentKind.BriefComment -> div("brief $additionalClasses") { childrenCallback() }
            node.dci.kind == ContentKind.Cover -> div("cover $additionalClasses") { childrenCallback() }
            node.hasStyle(TextStyle.Paragraph) -> p(additionalClasses) { childrenCallback() }
            node.hasStyle(TextStyle.Block) -> div(additionalClasses) { childrenCallback() }
            else -> childrenCallback()
        }
    }

    private fun FlowContent.wrapPlatformTagged(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        div("platform-tagged") {
            node.sourceSets.forEach {
                div("platform-tag") {
                    if (it.sourceSetName.equals("common", ignoreCase = true)) classes = classes + "common"
                    text(it.sourceSetName)
                }
            }
            div("content") {
                childrenCallback()
            }
        }
    }

    override fun FlowContent.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) =
        buildPlatformDependent(content.sourceSets.map { it to setOf(content.inner) }.toMap(), pageContext)

    private fun FlowContent.buildPlatformDependent(
        nodes: Map<SourceSetData, Collection<ContentNode>>,
        pageContext: ContentPage
    ) {
        div("platform-hinted") {
            attributes["data-platform-hinted"] = "data-platform-hinted"
            val contents = nodes.toList().mapIndexed { index, (sourceSet, elements) ->
                sourceSet to createHTML(prettyPrint = false).div(classes = "content") {
                    if (index == 0) attributes["data-active"] = ""
                    attributes["data-togglable"] = sourceSet.sourceSetName
                    elements.forEach {
                        buildContentNode(it, pageContext, setOf(sourceSet))
                    }
                }
            }

            if (contents.size != 1) {
                div("platform-bookmarks-row") {
                    attributes["data-toggle-list"] = "data-toggle-list"
                    contents.forEachIndexed { index, pair ->
                        button(classes = "platform-bookmark") {
                            if (index == 0) attributes["data-active"] = ""
                            attributes["data-toggle"] = pair.first.sourceSetName
                            text(pair.first.sourceSetName)
                        }
                    }
                }
            }
            contents.forEach {
                consumer.onTagContentUnsafe { +it.second }
            }
        }
    }

    override fun FlowContent.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {
        val distinct =
            node.children.map { instance ->
                instance to Pair(
                    createHTML(prettyPrint = false).div {
                        instance.before?.let { before ->
                            buildContentNode(before, pageContext, instance.sourceSets)
                        }
                    }.drop(5).dropLast(6),
                    createHTML(prettyPrint = false).div {
                        instance.after?.let { after ->
                            buildContentNode(after, pageContext, instance.sourceSets)
                        }
                    }.drop(5).dropLast(6)  // TODO: Find a way to do it without arbitrary trims
                )

            }.groupBy(
                Pair<ContentDivergentInstance, Pair<String, String>>::second,
                Pair<ContentDivergentInstance, Pair<String, String>>::first
            )

        distinct.forEach {
            consumer.onTagContentUnsafe { +it.key.first }
            consumer.onTagContentUnsafe {
                +createHTML(prettyPrint = false).div {
                    if (node.implicitlySourceSetHinted) {
                        buildPlatformDependent(
                            it.value.groupBy { it.sourceSets }
                                .flatMap { (sourceSets, elements) ->
                                    sourceSets.map { sourceSet -> sourceSet to elements.map { e -> e.divergent } }
                                }.toMap(),
                            pageContext
                        )
                    } else {
                        it.value.forEach {
                            buildContentNode(it.divergent, pageContext, null)
                        }
                    }
                }.drop(5).dropLast(6)
            }
            consumer.onTagContentUnsafe { +it.key.second }
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
            .filter {
                sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction }
            }
            .takeIf { it.isNotEmpty() }
            ?.let {
                div(classes = "table-row") {
                    it.filter { it.dci.kind != ContentKind.Symbol }.takeIf { it.isNotEmpty() }?.let {
                        div("main-subrow ${node.style.joinToString { it.toString().decapitalize() }}") {
                            it.filter { sourceSetRestriction == null || it.sourceSets.any { s -> s in sourceSetRestriction } }
                                .forEach {
                                    when(it.dci.kind){
                                        ContentKind.SourceSetDependantHint -> {
                                            div("platform-dependant-row keyValue"){
                                                div()
                                                div("title"){
                                                    it.build(this, pageContext, sourceSetRestriction)
                                                }
                                            }
                                        }
                                        ContentKind.Main -> {
                                            div("title-row"){
                                                it.build(this, pageContext, sourceSetRestriction)
                                                div()
                                                if (ContentKind.shouldBePlatformTagged(node.dci.kind) && node.sourceSets.size == 1) {
                                                    createPlatformTags(node)
                                                } else {
                                                    div()
                                                }
                                            }
                                        }
                                        else -> div { it.build(this, pageContext, sourceSetRestriction) }
                                    }
                                }
                        }
                    }
                    it.filter { it.dci.kind == ContentKind.Symbol }.takeIf { it.isNotEmpty() }?.let {
                        div("signature-subrow") {
                            div("signatures") {
                                it.forEach {
                                    it.build(this, pageContext, sourceSetRestriction)
                                }
                            }
                        }
                    }
                }
            }
    }


    private fun FlowContent.createPlatformTags(node: ContentNode) {
        div("platform-tags") {
            node.sourceSets.forEach {
                div("platform-tag") {
                    if (it.sourceSetName.equals("common", ignoreCase = true)) classes = classes + "common"
                    text(it.sourceSetName)
                }
            }
        }
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<SourceSetData>?
    ) {
        div(classes = "table") {
            node.children.forEach {
                buildRow(it, pageContext, sourceSetRestriction)
            }
        }
    }


    override fun FlowContent.buildHeader(level: Int, content: FlowContent.() -> Unit) {
        when (level) {
            1 -> h1(block = content)
            2 -> h2(block = content)
            3 -> h3(block = content)
            4 -> h4(block = content)
            5 -> h5(block = content)
            else -> h6(block = content)
        }
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

private fun PageNode.pageKind() = when (this) {
    is PackagePageNode -> "package"
    is ClasslikePageNode -> "class"
    is MemberPageNode -> when (this.documentable) {
        is DFunction -> "function"
        else -> "other"
    }
    else -> "other"
}

private val PageNode.isNavigable: Boolean
    get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing