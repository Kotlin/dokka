package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.resolvers.LocationProvider


abstract class DefaultRenderer<T>(
    protected val outputWriter: OutputWriter,
    protected val context: DokkaContext
) : Renderer {

    protected lateinit var locationProvider: LocationProvider
        private set

    protected abstract fun T.buildHeader(level: Int, content: T.() -> Unit)
    protected abstract fun T.buildLink(address: String, content: T.() -> Unit)
    protected abstract fun T.buildList(node: ContentList, pageContext: PageNode)
    protected abstract fun T.buildNewLine()
    protected abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: PageNode)
    protected abstract fun T.buildTable(node: ContentTable, pageContext: PageNode)
    protected abstract fun T.buildText(textNode: ContentText)
    protected abstract fun T.buildNavigation(page: PageNode)

    protected abstract fun buildPage(page: PageNode, content: (T, PageNode) -> Unit): String
    protected abstract fun buildError(node: ContentNode)

    protected open fun T.buildGroup(node: ContentGroup, pageContext: PageNode){
        node.children.forEach { it.build(this, pageContext) }
    }

    protected open fun T.buildLinkText(nodes: List<ContentNode>, pageContext: PageNode) {
        nodes.forEach { it.build(this, pageContext) }
    }

    protected open fun T.buildCode(code: List<ContentNode>, language: String, pageContext: PageNode) {
        code.forEach { it.build(this, pageContext) }
    }

    protected open fun T.buildHeader(node: ContentHeader, pageContext: PageNode) {
        buildHeader(node.level) { node.children.forEach { it.build(this, pageContext) } }
    }

    protected open fun ContentNode.build(builder: T, pageContext: PageNode) = builder.buildContentNode(this, pageContext)

    protected open fun T.buildContentNode(node: ContentNode, pageContext: PageNode) {
        when (node) {
            is ContentText -> buildText(node)
            is ContentHeader -> buildHeader(node, pageContext)
            is ContentCode -> buildCode(node.children, node.language, pageContext)
            is ContentDRILink -> buildLink(
                locationProvider.resolve(node.address, node.platforms.toList(), pageContext)) {
                buildLinkText(node.children, pageContext)
            }
            is ContentResolvedLink -> buildLink(node.address) {buildLinkText(node.children, pageContext)}
            is ContentEmbeddedResource -> buildResource(node, pageContext)
            is ContentList -> buildList(node, pageContext)
            is ContentTable -> buildTable(node, pageContext)
            is ContentGroup -> buildGroup(node, pageContext)
            else -> buildError(node)
        }
    }

    protected open fun buildPageContent(context: T, page: PageNode) {
        context.buildNavigation(page)
        page.content.build(context, page)
    }

    protected open fun renderPage(page: PageNode) =
        outputWriter.write(locationProvider.resolve(page), buildPage(page, ::buildPageContent), "")

    protected open fun renderPages(root: PageNode) {
        renderPage(root)
        root.children.forEach { renderPages(it) }
    }

    protected open fun buildSupportFiles() {}

    protected open fun renderPackageList(root: PageNode) =
        getPackageNamesAndPlatforms(root)
            .keys
            .joinToString("\n")
            .also { outputWriter.write("${root.name}/package-list", it, "") }

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
        locationProvider = context.single(CoreExtensions.locationProviderFactory).getLocationProvider(root as ModulePageNode)
        renderPackageList(root)
        buildSupportFiles()
        renderPages(root)
    }
}

fun PageNode.platforms() = this.content.platforms.toList()