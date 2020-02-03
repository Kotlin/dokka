package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.resolvers.LocationProvider
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer

abstract class DefaultRenderer<T>(
    protected val outputWriter: OutputWriter,
    protected val context: DokkaContext
) : Renderer {

    private val extension = context.single(CoreExtensions.fileExtension)

    protected lateinit var locationProvider: LocationProvider
        private set

    protected open val preprocessors: Iterable<PageNodeTransformer> = emptyList()

    protected abstract fun T.buildHeader(level: Int, content: T.() -> Unit)
    protected abstract fun T.buildLink(address: String, content: T.() -> Unit)
    protected abstract fun T.buildList(node: ContentList, pageContext: ContentPage)
    protected abstract fun T.buildNewLine()
    protected abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage)
    protected abstract fun T.buildTable(node: ContentTable, pageContext: ContentPage)
    protected abstract fun T.buildText(textNode: ContentText)
    protected abstract fun T.buildNavigation(page: PageNode)

    protected abstract fun buildPage(page: ContentPage, content: (T, ContentPage) -> Unit): String
    protected abstract fun buildError(node: ContentNode)

    protected open fun T.buildGroup(node: ContentGroup, pageContext: ContentPage) {
        node.children.forEach { it.build(this, pageContext) }
    }

    protected open fun T.buildLinkText(nodes: List<ContentNode>, pageContext: ContentPage) {
        nodes.forEach { it.build(this, pageContext) }
    }

    protected open fun T.buildCode(code: List<ContentNode>, language: String, pageContext: ContentPage) {
        code.forEach { it.build(this, pageContext) }
    }

    protected open fun T.buildHeader(node: ContentHeader, pageContext: ContentPage) {
        buildHeader(node.level) { node.children.forEach { it.build(this, pageContext) } }
    }

    protected open fun ContentNode.build(builder: T, pageContext: ContentPage) =
        builder.buildContentNode(this, pageContext)

    protected open fun T.buildContentNode(node: ContentNode, pageContext: ContentPage) {
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

    protected open fun buildPageContent(context: T, page: ContentPage) {
        context.buildNavigation(page)
        page.content.build(context, page)
    }

    protected open fun renderPage(page: PageNode) {
        val path by lazy { locationProvider.resolve(page, skipExtension = true) }
        when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, extension)
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page))
                RenderingStrategy.DoNothing -> Unit
            }
            else -> throw AssertionError(
                "Page ${page.name} cannot be rendered by renderer as it is not renderer specific nor contains content"
            )
        }
    }

    protected open fun renderPages(root: PageNode) {
        renderPage(root)
        root.children.forEach { renderPages(it) }
    }

    // reimplement this as preprocessor
    protected open fun renderPackageList(root: ContentPage) =
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

    override fun render(root: RootPageNode) {
        val newRoot = preprocessors.fold(root) { acc, t -> t(acc) }

        locationProvider =
            context.single(CoreExtensions.locationProviderFactory).getLocationProvider(newRoot)

        root.children<ModulePageNode>().forEach { renderPackageList(it) }

        renderPages(newRoot)
    }
}

fun ContentPage.platforms() = this.content.platforms.toList()