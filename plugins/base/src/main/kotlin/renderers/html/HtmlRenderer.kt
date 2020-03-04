package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

open class HtmlRenderer(
    context: DokkaContext
) : DefaultRenderer<FlowContent>(context) {

    private val pageList = mutableListOf<String>()

    override val preprocessors = listOf(
        RootCreator,
        SearchPageInstaller,
        ResourceInstaller,
        NavigationPageInstaller,
        StyleAndScriptsAppender
    )

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) = when {
        node.style.contains(TextStyle.Paragraph) -> p { childrenCallback() }
        node.style.contains(TextStyle.Block) -> div { childrenCallback() }
        else -> childrenCallback()
    }

    override fun FlowContent.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) {
        val distinct = content.platforms.map {
            it to createHTML(prettyPrint = false).div {
                buildContentNode(content.inner, pageContext, it)
            }.drop(5).dropLast(6) // TODO: Find a way to do it without arbitrary trims
        }.groupBy(Pair<PlatformData, String>::second, Pair<PlatformData, String>::first)

        if (distinct.size == 1)
            consumer.onTagContentUnsafe { +distinct.keys.single() }
        else
            distinct.forEach { text, platforms ->
                consumer.onTagContentUnsafe { +platforms.joinToString(prefix = "$text [", postfix = "]") { it.name } }
            }
    }

    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        platformRestriction: PlatformData?
    ) = if (node.ordered) ol { buildListItems(node.children, pageContext, platformRestriction) }
    else ul { buildListItems(node.children, pageContext, platformRestriction) }

    open fun OL.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        platformRestriction: PlatformData? = null
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
        platformRestriction: PlatformData? = null
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
            val imgAttrs = node.extras.filterIsInstance<HTMLSimpleAttr>().joinAttr()
            img(src = node.address, alt = node.altText)
        } else {
            println("Unrecognized resource type: $node")
        }
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        platformRestriction: PlatformData?
    ) {
        table {
            thead {
                node.header.forEach {
                    tr {
                        it.children.forEach {
                            th {
                                it.build(this@table, pageContext, platformRestriction)
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
                                it.build(this, pageContext, platformRestriction)
                            }
                        }
                    }
                }
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
        locationProvider.ancestors(page).asReversed().forEach { node ->
            text("/")
            if (node.isNavigable) buildLink(node, page)
            else text(node.name)
        }

    private fun FlowContent.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)) {
            text(to.name)
        }

    fun FlowContent.buildLink(
        to: DRI,
        platforms: List<PlatformData>,
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
        buildNewLine()
        code.forEach {
            +((it as? ContentText)?.text ?: run { context.logger.error("Cannot cast $it as ContentText!"); "" })
            buildNewLine()
        }
    }

    override fun renderPage(page: PageNode) {
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
        text(textNode.text)
    }

    override fun render(root: RootPageNode) {
        super.render(root)
        outputWriter.write("scripts/pages", "var pages = [\n${pageList.joinToString(",\n")}\n]", ".js")
    }

    private fun PageNode.root(path: String) = locationProvider.resolveRoot(this) + path

    override fun buildPage(page: ContentPage, content: (FlowContent, ContentPage) -> Unit): String =
        buildHtml(page, page.embeddedResources) { content(this, page) }

    open fun buildHtml(page: PageNode, resources: List<String>, content: FlowContent.() -> Unit) =
        createHTML().html {
            head {
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
                    id = "navigation"
                    div {
                        id = "searchBar"
                        form(action = page.root("-search.html"), method = FormMethod.get) {
                            id = "searchForm"
                            input(type = InputType.search, name = "query")
                            input(type = InputType.submit) { value = "Search" }
                        }
                    }
                    div {
                        id = "sideMenu"
                    }
                }
                div {
                    id = "content"
                    content()
                }
            }
        }
}

fun List<HTMLMetadata>.joinAttr() = joinToString(" ") { it.key + "=" + it.value }

private fun PageNode.pageKind() = when (this) {
    is PackagePageNode -> "package"
    is ClasslikePageNode -> "class"
    is MemberPageNode -> when (this.documentable) {
        is Function -> "function"
        else -> "other"
    }
    else -> "other"
}

private val PageNode.isNavigable: Boolean
    get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing