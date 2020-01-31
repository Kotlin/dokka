package org.jetbrains.dokka.renderers

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

open class HtmlRenderer(
    outputWriter: OutputWriter,
    context: DokkaContext
) : DefaultRenderer<FlowContent>(outputWriter, context) {

    private val pageList = mutableListOf<String>()

    private var idCounter = 0
        get() = ++field

    private fun FlowContent.buildSideMenu(context: PageNode, node: PageNode) {
        val children = node.children.filter { it !is MemberPageNode }
        val submenuId = if (children.isNotEmpty()) "nav$idCounter" else null
        div("sideMenuPart") {
            submenuId?.also { id = it }
            div("overview") {
                buildLink(node, context)
                submenuId?.also {
                    span("navButton") {
                        onClick = """document.getElementById("$it").classList.toggle("hidden");"""
                        span("navButtonContent")
                    }
                }
            }
            children.forEach { buildSideMenu(context, it) }
        }
    }

    override val preprocessors = listOf(RootCreator, SearchPageInstaller, ResourceInstaller, StyleAndScriptsAppender)

    override fun FlowContent.buildList(node: ContentList, pageContext: ContentPage) =
        if (node.ordered) ol {
            buildListItems(node.children, pageContext)
        }
        else ul {
            buildListItems(node.children, pageContext)
        }

    protected open fun OL.buildListItems(items: List<ContentNode>, pageContext: ContentPage) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext) }
        }
    }

    protected open fun UL.buildListItems(items: List<ContentNode>, pageContext: ContentPage) {
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

    override fun FlowContent.buildTable(node: ContentTable, pageContext: ContentPage) {
        table {
            thead {
                node.header.forEach {
                    tr {
                        it.children.forEach {
                            th {
                                it.build(this@table, pageContext)
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
                                it.build(this, pageContext)
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
            else -> h5(block = content)
        }
    }

    override fun FlowContent.buildNavigation(page: PageNode) =
        locationProvider.ancestors(page).forEach { node ->
            text("/")
            buildLink(node, page)
        }

    private fun FlowContent.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)) {
            text(to.name)
        }

    override fun buildError(node: ContentNode) {
        context.logger.error("Unknown ContentNode type: $node")
    }

    override fun FlowContent.buildNewLine() {
        br()
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) =
        a(href = address, block = content)

    override fun FlowContent.buildCode(code: List<ContentNode>, language: String, pageContext: ContentPage) {
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
                """{ "name": "${page.name}", ${if (page is ClassPageNode) "\"class\": \"${page.name}\"," else ""} "location": "${locationProvider.resolve(
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
                    filter { it.substringAfterLast('.') == "css" }
                        .forEach { link(rel = LinkRel.stylesheet, href = page.root(it)) }
                    filter { it.substringAfterLast('.') == "js" }
                        .forEach { script(type = ScriptType.textJavaScript, src = it) { async = true } }
                }
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
    is ClassPageNode -> "class"
    is MemberPageNode -> when (this.documentable) {
        is Function -> "function"
        else -> "other"
    }
    else -> "other"
}
