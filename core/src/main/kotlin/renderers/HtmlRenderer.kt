package org.jetbrains.dokka.renderers

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.resolvers.LocationProvider
import java.io.File
import java.net.URL

open class HtmlRenderer(
    writer: Writer,
    locationProvider: LocationProvider,
    context: DokkaContext
) : DefaultRenderer<FlowContent>(writer, locationProvider, context) {

    override fun FlowContent.buildList(node: ContentList, pageContext: PageNode) =
        if (node.ordered) ol {
            buildListItems(node.children, pageContext)
        }
        else ul {
            buildListItems(node.children, pageContext)
        }

    protected open fun OL.buildListItems(items: List<ContentNode>, pageContext: PageNode) {
        items.forEach {
            if (it is ContentText)
                li { it.build(this, pageContext) }
            else buildList(it as ContentList, pageContext)
        }
    }

    protected open fun UL.buildListItems(items: List<ContentNode>, pageContext: PageNode) {
        items.forEach {
            if (it is ContentText)
                li { it.build(this, pageContext) }
            else buildList(it as ContentList, pageContext)
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

    override fun FlowContent.buildNavigation(page: PageNode) {
        locationProvider.ancestors(page).forEach { node ->
            text("/")
            buildLink(locationProvider.resolve(node, page)) {
                text(node.name)
            }
        }
    }

    override fun buildError(node: ContentNode) {
        context.logger.error("Unknown ContentNode type: $node")
    }

    override fun FlowContent.buildNewLine() { br() }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) {
        a (href = address, block = content)
    }

    override fun FlowContent.buildCode(code: List<ContentNode>, language: String, pageContext: PageNode) {
        buildNewLine()
        code.forEach {
            + (it as ContentText).text
            buildNewLine()
        }
    }

    override fun FlowContent.buildText(textNode: ContentText) {
        text(textNode.text)
    }

    override fun buildSupportFiles() { // TODO copy file instead of reading
        writer.write(
            "style.css",
            javaClass.getResourceAsStream("/dokka/styles/style.css").reader().readText()
        )
    }

    override fun buildPage(page: PageNode, content: (FlowContent, PageNode) -> Unit): String = StringBuilder().appendHTML().html {
        head {
            title(page.name)
            link(rel = LinkRel.stylesheet, href = "${locationProvider.resolveRoot(page)}style.css")
            page.embeddedResources.filter { URL(it).path.substringAfterLast('.') == "js" }
                .forEach { script(type = ScriptType.textJavaScript, src = it) { async = true } }
        }
        body {
            content(this, page)
        }
    }.toString()

    protected open fun List<HTMLMetadata>.joinAttr() = this.joinToString(" ") { it.key + "=" + it.value }
}