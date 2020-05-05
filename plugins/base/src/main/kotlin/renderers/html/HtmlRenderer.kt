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

    override fun FlowContent.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) {
        div("platform-hinted") {
            attributes["data-platform-hinted"] = "data-platform-hinted"
            val contents = content.sourceSets.mapIndexed { index, platform ->
                platform to createHTML(prettyPrint = false).div(classes = "content") {
                    if (index == 0) attributes["data-active"] = ""
                    attributes["data-togglable"] = platform.sourceSetName
                    buildContentNode(content.inner, pageContext, platform)
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

    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        platformRestriction: SourceSetData?
    ) = if (node.ordered) ol { buildListItems(node.children, pageContext, platformRestriction) }
    else ul { buildListItems(node.children, pageContext, platformRestriction) }

    open fun OL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        platformRestriction: SourceSetData? = null
    ) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext, platformRestriction) }
        }
    }

    open fun UL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        platformRestriction: SourceSetData? = null
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
        platformRestriction: SourceSetData?
    ) {
        node.children
            .filter {
                platformRestriction == null || platformRestriction in it.sourceSets
            }
            .takeIf { it.isNotEmpty() }
            ?.let {
                div(classes = "table-row") {
                    it.filter { it.dci.kind != ContentKind.Symbol }.takeIf { it.isNotEmpty() }?.let {
                        div("main-subrow") {
                            it.filter { platformRestriction == null || platformRestriction in it.sourceSets }
                                .forEach {
                                    when (it.dci.kind) {
                                        ContentKind.Main -> div("title") {
                                            it.build(this, pageContext, platformRestriction)
                                        }
                                        ContentKind.BriefComment, ContentKind.Comment -> div("brief") {
                                            it.build(this, pageContext, platformRestriction)
                                        }
                                        else -> div { it.build(this, pageContext, platformRestriction) }
                                    }
                                }
                            if (ContentKind.shouldBePlatformTagged(node.dci.kind) && node.sourceSets.size == 1) {
                                createPlatformTags(node)
                            }
                        }
                    }
                    it.filter { it.dci.kind == ContentKind.Symbol }.takeIf { it.isNotEmpty() }?.let {
                        div("signature-subrow") {
                            div("signatures") {
                                it.forEach {
                                    it.build(this, pageContext, platformRestriction)
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
        platformRestriction: SourceSetData?
    ) {
        div(classes = "table") {
            node.children.forEach {
                buildRow(it, pageContext, platformRestriction)
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