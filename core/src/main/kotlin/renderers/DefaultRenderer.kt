package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.resolvers.LocationProvider

abstract class DefaultRenderer(val fileWriter: FileWriter, val locationProvider: LocationProvider): Renderer {

    protected abstract fun buildHeader(level: Int, text: String): String
    protected abstract fun buildNewLine(): String
    protected abstract fun buildLink(text: String, address: String): String
    protected abstract fun buildCode(code: String): String
    protected abstract fun buildNavigation(): String // TODO
    protected open fun buildText(text: String): String = text
    protected open fun buildHeader(level: Int, content: List<ContentNode>, pageContext: PageNode): String = buildHeader(level, content.joinToString { it.build(pageContext) })
    protected open fun buildGroup(children: List<ContentNode>, pageContext: PageNode): String = children.joinToString { it.build(pageContext) }
    protected open fun buildComment(parts: List<ContentNode>, pageContext: PageNode): String = parts.joinToString { it.build(pageContext) }
    protected open fun buildSymbol(parts: List<ContentNode>, pageContext: PageNode): String = parts.joinToString(separator = "") { it.build(pageContext) }
    protected open fun buildBlock(name: String, content: List<ContentNode>, pageContext: PageNode) = buildHeader(3, name) + content.joinToString("\n") { it.build(pageContext) }

    protected open fun ContentNode.build(pageContext: PageNode): String = buildContentNode(this, pageContext)

    protected open fun buildContentNode(node: ContentNode, pageContext: PageNode): String =
        when(node) {
            is ContentText -> buildText(node.text)
            is ContentComment -> buildComment(node.parts, pageContext)
            is ContentSymbol -> buildSymbol(node.parts, pageContext)
            is ContentCode -> buildCode(node.code)
            is ContentBlock -> buildBlock(node.name, node.children, pageContext)
            is ContentLink -> buildLink(node.text, locationProvider.resolve(node.address, node.platforms, pageContext))
            is ContentGroup -> buildGroup(node.children, pageContext)
            is ContentHeader -> buildHeader(node.level, node.items, pageContext)
            is ContentStyle -> node.items.joinToString(separator = "\n") { buildContentNode(it, pageContext) }
            else -> ""
        }

    protected open fun buildPageContent(page: PageNode): String =
        /*buildHeader(1, page.name) + */ page.content.joinToString("\n") { it.build(page) }

    protected open fun renderPage(page: PageNode) = fileWriter.write(locationProvider.resolve(page), buildPageContent(page), "")

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
