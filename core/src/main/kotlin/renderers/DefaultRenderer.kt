package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.resolvers.LocationProvider

abstract class DefaultRenderer(val fileWriter: FileWriter, val locationProvider: LocationProvider) : Renderer {

    protected abstract fun buildHeader(level: Int, text: String): String
    protected abstract fun buildLink(text: String, address: String): String
    protected abstract fun buildList(node: ContentList, pageContext: PageNode): String
    protected abstract fun buildNewLine(): String
    protected abstract fun buildResource(node: ContentEmbeddedResource, pageContext: PageNode): String
    protected abstract fun buildTable(node: ContentTable, pageContext: PageNode): String

    protected open fun buildText(textNode: ContentText): String = textNode.text

    protected open fun buildGroup(node: ContentGroup, pageContext: PageNode): String = node.children.joinToString("") { it.build(pageContext) }

    protected open fun buildLinkText(nodes: List<ContentNode>, pageContext: PageNode): String =
        nodes.joinToString(" ") { it.build(pageContext) }

    protected open fun buildCode(code: List<ContentNode>, language: String, pageContext: PageNode): String =
        code.joinToString { it.build(pageContext) }

    protected open fun buildHeader(node: ContentHeader, pageContext: PageNode): String =
        buildHeader(node.level, node.children.joinToString { it.build(pageContext) })

    protected open fun buildNavigation(page: PageNode): String {
        fun buildNavigationWithContext(page: PageNode, context: PageNode): String =
            page.parent?.let { buildNavigationWithContext(it, context) }.orEmpty() + "/" + buildLink(
                page.name,
                locationProvider.resolve(page, context)
            )
        return buildNavigationWithContext(page, page)
    }

    protected open fun ContentNode.build(pageContext: PageNode): String = buildContentNode(this, pageContext)

    protected open fun buildContentNode(node: ContentNode, pageContext: PageNode): String =
        when (node) {
            is ContentText -> buildText(node)
            is ContentHeader -> buildHeader(node, pageContext)
            is ContentCode -> buildCode(node.children, node.language, pageContext)
            is ContentDRILink -> buildLink(
                buildLinkText(node.children, pageContext),
                locationProvider.resolve(node.address, node.platforms.toList(), pageContext)
            )
            is ContentResolvedLink -> buildLink(buildLinkText(node.children, pageContext), node.address)
            is ContentEmbeddedResource -> buildResource(node, pageContext)
            is ContentList -> buildList(node, pageContext)
            is ContentTable -> buildTable(node, pageContext)
            is ContentGroup -> buildGroup(node, pageContext)
            else -> "".also { println("Unrecognized ContentNode: $node") }
        }

    protected open fun buildPageContent(page: PageNode): String =
        buildNavigation(page) + page.content.build(page)

    protected open fun renderPage(page: PageNode) =
        fileWriter.write(locationProvider.resolve(page), buildPageContent(page), "")

    protected open fun renderPages(root: PageNode) {
        renderPage(root)
        root.children.forEach { renderPages(it) }
    }

    protected open fun buildSupportFiles() {}

    protected open fun renderPackageList(root: PageNode) =
        getPackageNamesAndPlatforms(root)
            .keys
            .joinToString("\n")
            .also { fileWriter.write("package-list", it, "") }

    protected open fun getPackageNamesAndPlatforms(root: PageNode): Map<String, List<PlatformData>> =
        root.children
            .map(::getPackageNamesAndPlatforms)
            .fold(emptyMap<String, List<PlatformData>>()) { e, acc -> acc + e } +
                if (root is PackagePageNode) {
                    mapOf(root.name to root.platforms())
                } else {
                    emptyMap()
                }

    override fun render(root: PageNode) {
        renderPackageList(root)
        buildSupportFiles()
        renderPages(root)
    }
}

fun PageNode.platforms() = this.content.platforms.toList()