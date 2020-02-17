package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.OutputWriter
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.resolvers.LocationProvider
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer

abstract class DefaultRenderer<T>(
    protected val context: DokkaContext
) : Renderer {

    protected val outputWriter = context.single(CoreExtensions.outputWriter)

    protected lateinit var locationProvider: LocationProvider
        private set

    protected open val preprocessors: Iterable<PageNodeTransformer> = emptyList()

    abstract fun T.buildHeader(level: Int, content: T.() -> Unit)
    abstract fun T.buildLink(address: String, content: T.() -> Unit)
    abstract fun T.buildList(node: ContentList, pageContext: ContentPage)
    abstract fun T.buildNewLine()
    abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage)
    abstract fun T.buildTable(node: ContentTable, pageContext: ContentPage)
    abstract fun T.buildText(textNode: ContentText)
    abstract fun T.buildNavigation(page: PageNode)

    abstract fun buildPage(page: ContentPage, content: (T, ContentPage) -> Unit): String
    abstract fun buildError(node: ContentNode)

    open fun T.buildGroup(node: ContentGroup, pageContext: ContentPage) {
        node.children.forEach { it.build(this, pageContext) }
    }

    open fun T.buildLinkText(nodes: List<ContentNode>, pageContext: ContentPage) {
        nodes.forEach { it.build(this, pageContext) }
    }

    open fun T.buildCode(code: List<ContentNode>, language: String, pageContext: ContentPage) {
        code.forEach { it.build(this, pageContext) }
    }

    open fun T.buildHeader(node: ContentHeader, pageContext: ContentPage) {
        buildHeader(node.level) { node.children.forEach { it.build(this, pageContext) } }
    }

    open fun ContentNode.build(builder: T, pageContext: ContentPage) =
        builder.buildContentNode(this, pageContext)

    open fun T.buildContentNode(node: ContentNode, pageContext: ContentPage) {
        when (node) {
            is ContentText -> buildText(node)
            is ContentHeader -> buildHeader(node, pageContext)
            is ContentCode -> buildCode(node.children, node.language, pageContext)
            is ContentDRILink -> buildLink(
                locationProvider.resolve(node.address, node.platforms.toList(), pageContext)
            ) {
                buildLinkText(node.children, pageContext)
            }
            is ContentResolvedLink -> buildLink(node.address) { buildLinkText(node.children, pageContext) }
            is ContentEmbeddedResource -> buildResource(node, pageContext)
            is ContentList -> buildList(node, pageContext)
            is ContentTable -> buildTable(node, pageContext)
            is ContentGroup -> buildGroup(node, pageContext)
            else -> buildError(node)
        }
    }

    open fun buildPageContent(context: T, page: ContentPage) {
        context.buildNavigation(page)
        page.content.build(context, page)
    }

    open fun renderPage(page: PageNode) {
        val path by lazy { locationProvider.resolve(page, skipExtension = true) }
        when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".html")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".html")
                RenderingStrategy.DoNothing -> Unit
            }
            else -> throw AssertionError(
                "Page ${page.name} cannot be rendered by renderer as it is not renderer specific nor contains content"
            )
        }
    }

    open fun renderPages(root: PageNode) {
        renderPage(root)
        root.children.forEach { renderPages(it) }
    }

    // reimplement this as preprocessor
    open fun renderPackageList(root: ContentPage) =
        getPackageNamesAndPlatforms(root)
            .keys
            .joinToString("\n")
            .also { outputWriter.write("${root.name}/package-list", it, "") }

    open fun getPackageNamesAndPlatforms(root: PageNode): Map<String, List<PlatformData>> =
        root.children
            .map(::getPackageNamesAndPlatforms)
            .fold(emptyMap<String, List<PlatformData>>()) { e, acc -> acc + e } +
                if (root is PackagePageNode) {
                    mapOf(root.name to root.platforms())
                } else {
                    emptyMap()
                }

    override fun render(root: RootPageNode) {
        val newRoot = preprocessors.fold(root) { acc, t -> t(acc) }

        locationProvider =
            context.single(CoreExtensions.locationProviderFactory).getLocationProvider(newRoot)

        root.children<ModulePageNode>().forEach { renderPackageList(it) }

        renderPages(newRoot)
    }
}

fun ContentPage.platforms() = this.content.platforms.toList()