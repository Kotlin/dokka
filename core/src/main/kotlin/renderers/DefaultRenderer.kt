package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.resolvers.LocationProvider

abstract class DefaultRenderer(val outputDir: String, val fileWriter: FileWriter, val locationProvider: LocationProvider): Renderer {

    protected abstract fun buildHeader(level: Int, text: String): String
    protected abstract fun buildNewLine(): String
    protected abstract fun buildLink(text: String, address: String): String
    protected abstract fun buildCode(code: String): String
    protected abstract fun buildNavigation(): String // TODO
    protected open fun buildText(text: String): String = text
    protected open fun buildGroup(children: List<ContentNode>): String = children.joinToString { it.build() }
    protected open fun buildComment(parts: List<ContentNode>): String = parts.joinToString { it.build() }
    protected open fun buildSymbol(parts: List<ContentNode>): String = parts.joinToString { it.build() }
    protected open fun buildBlock(name: String, content: List<ContentNode>) = buildHeader(2, name) + content.joinToString("\n") { it.build() }

    protected open fun ContentNode.build(): String = buildContentNode(this)

    protected open fun buildContentNode(node: ContentNode) =
        when(node) {
            is ContentText -> buildText(node.text)
            is ContentComment -> buildComment(node.parts)
            is ContentSymbol -> buildSymbol(node.parts)
            is ContentCode -> buildCode(node.code)
            is ContentBlock -> buildBlock(node.name, node.children)
            is ContentLink -> buildLink(node.text, locationProvider.resolve(node.address, node.platforms))
            is ContentGroup -> buildGroup(node.children)
            else -> ""
        }

    protected open fun buildPageContent(page: PageNode): String =
        buildHeader(1, page.name) + page.content.joinToString("\n") { it.build() }

    protected open fun renderPage(page: PageNode) = fileWriter.write(locationProvider.resolve(page), buildPageContent(page))

    protected open fun renderPages(root: PageNode) {
        renderPage(root)
        root.children.forEach { renderPages(it) }
    }

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
        renderPages(root)
    }
}
