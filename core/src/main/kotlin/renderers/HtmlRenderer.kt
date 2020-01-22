package org.jetbrains.dokka.renderers

import kotlinx.html.*
import kotlinx.html.dom.document
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Parameter
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.net.URL

open class HtmlRenderer(
    outputWriter: OutputWriter,
    context: DokkaContext
) : DefaultRenderer<FlowContent>(outputWriter, context) {

    private val pageList = mutableListOf<String>()

    private var idCounter = 0
        get() = ++field

    private fun FlowContent.buildSideMenu(context: PageNode, node: PageNode) {
        val children = node.children.filter { it !is MemberPageNode }
        val className = children.ifNotEmpty { "nav$idCounter" }
        div("sideMenuPart") {
            className?.let { id = it }
            div("overview") {
                buildLink(node, context)
                className?.let {
                    span("navButton") {
                        onClick = """document.getElementById("$it").classList.toggle("hidden");"""
                        span("navButtonContent")
                    }
                }
            }
            children.forEach { buildSideMenu(context, it) }
        }
    }

    override fun FlowContent.buildList(node: ContentList, pageContext: PageNode) =
        if (node.ordered) ol {
            buildListItems(node.children, pageContext)
        }
        else ul {
            buildListItems(node.children, pageContext)
        }

    protected open fun OL.buildListItems(items: List<ContentNode>, pageContext: PageNode) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext) }
        }
    }

    protected open fun UL.buildListItems(items: List<ContentNode>, pageContext: PageNode) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext) }
        }
    }

    override fun FlowContent.buildResource(
        node: ContentEmbeddedResource,
        pageContext: PageNode
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

    override fun FlowContent.buildTable(node: ContentTable, pageContext: PageNode) {
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

    override fun FlowContent.buildCode(code: List<ContentNode>, language: String, pageContext: PageNode) {
        buildNewLine()
        code.forEach {
            +((it as? ContentText)?.text ?: run { context.logger.error("Cannot cast $it as ContentText!"); "" })
            buildNewLine()
        }
    }

    override fun renderPage(page: PageNode) {
        super.renderPage(page)
        pageList.add(
            """{ "name": "${page.name}", ${if (page is ClasslikePageNode) "\"class\": \"${page.name}\"," else ""} "location": "${locationProvider.resolve(
                page
            )}", "kind": "${page.pageKind()}" }"""
        )
    }

    override fun FlowContent.buildText(textNode: ContentText) {
        text(textNode.text)
    }

    override fun render(root: PageNode) {
        super.render(root)
        outputWriter.write("scripts/pages", "var pages = [\n${pageList.joinToString(",\n")}\n]", ".js")
    }

    override fun buildSupportFiles() { // TODO copy file instead of reading
        outputWriter.write(
            "style.css",
            javaClass.getResourceAsStream("/dokka/styles/style.css").reader().readText()
        )
        renderPage(searchPageNode)
        outputWriter.writeResources("/dokka/styles", "styles")
        outputWriter.writeResources("/dokka/scripts", "scripts")
        outputWriter.writeResources("/dokka/images", "images")
    }

    private fun PageNode.root(path: String) =
        "${if (this != searchPageNode) locationProvider.resolveRoot(this) else ""}$path"

    override fun buildPage(page: PageNode, content: (FlowContent, PageNode) -> Unit): String =
        StringBuilder().appendHTML().html {
            document {

            }
            head {
                title(page.name)
                link(rel = LinkRel.stylesheet, href = page.root("styles/style.css"))
                page.embeddedResources.filter {
                    URL(it).path.substringAfterLast('.') == "js"
                }.forEach {
                    script(type = ScriptType.textJavaScript, src = it) { async = true }
                }
                script(
                    type = ScriptType.textJavaScript,
                    src = page.root("scripts/pages.js")
                ) { async = true }

            }
            body {
                div {
                    id = "navigation"
                    div {
                        id = "searchBar"
                    }
                    div {
                        id = "sideMenu"
                        img(src = page.root("images/logo-icon.svg"))
                        img(src = page.root("images/logo-text.svg"))
                        hr()
                        input(type = InputType.search) {
                            id = "navigationFilter"
                        }
                        script(
                            type = ScriptType.textJavaScript,
                            src = page.root("scripts/scripts.js")
                        ) { async = true }
                        buildSideMenu(page, locationProvider.top())
                    }
                }
                div {
                    id = "content"
                    if (page != searchPageNode) {
                        content(this, page)
                        script(
                            type = ScriptType.textJavaScript,
                            src = page.root("scripts/searchConfig.js")
                        ) { async = true }
                        script(
                            type = ScriptType.textJavaScript,
                            src = page.root("scripts/fuzzySearch.js")
                        ) { async = true }
                    } else {
                        h1 {
                            id = "searchTitle"
                            text("Search results for ")
                        }
                        table {
                            tbody {
                                id = "searchTable"
                            }
                        }
                        script(
                            type = ScriptType.textJavaScript,
                            src = page.root("scripts/search.js")
                        ) { async = true }
                        script(
                            type = ScriptType.textJavaScript,
                            src = page.root("scripts/searchConfig.js")
                        ) { async = true }
                        script(
                            type = ScriptType.textJavaScript,
                            src = page.root("scripts/fuzzySearch.js")
                        ) { async = true }
                    }
                }
            }
        }.toString()

    protected open fun List<HTMLMetadata>.joinAttr() = this.joinToString(" ") { it.key + "=" + it.value }

    private val searchPageNode =
        object : PageNode {
            override val name: String
                get() = "Search"
            override val content = object : ContentNode {
                override val dci: DCI = DCI(DRI.topLevel, ContentKind.Main)
                override val platforms: Set<PlatformData> = emptySet()
                override val style: Set<Style> = emptySet()
                override val extras: Set<Extra> = emptySet()

            }
            override val dri: DRI = DRI.topLevel
            override val documentable: Documentable? = null
            override val embeddedResources: List<String> = emptyList()
            override val children: List<PageNode> = emptyList()

            override fun modified(
                name: String,
                content: ContentNode,
                embeddedResources: List<String>,
                children: List<PageNode>
            ): PageNode = this

        }

    private fun PageNode.pageKind() = when (this) {
        is PackagePageNode -> "package"
        is ClasslikePageNode -> "class"
        is MemberPageNode -> when (this.documentable) {
            is Function -> "function"
            else -> "other"
        }
        else -> "other"
    }
}