package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.pages.PageTransformer

abstract class DefaultRenderer<T>(
    protected val context: DokkaContext
) : Renderer {

    protected val outputWriter = context.plugin<DokkaBase>().querySingle { outputWriter }

    protected lateinit var locationProvider: LocationProvider
        private set

    protected open val preprocessors: Iterable<PageTransformer> = emptyList()

    abstract fun T.buildHeader(level: Int, content: T.() -> Unit)
    abstract fun T.buildLink(address: String, content: T.() -> Unit)
    abstract fun T.buildList(node: ContentList, pageContext: ContentPage, platformRestriction: PlatformData? = null)
    abstract fun T.buildNewLine()
    abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage)
    abstract fun T.buildTable(node: ContentTable, pageContext: ContentPage, platformRestriction: PlatformData? = null)
    abstract fun T.buildText(textNode: ContentText)
    abstract fun T.buildNavigation(page: PageNode)

    abstract fun buildPage(page: ContentPage, content: (T, ContentPage) -> Unit): String
    abstract fun buildError(node: ContentNode)

    open fun T.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) =
        buildContentNode(content.inner, pageContext)

    open fun T.buildGroup(node: ContentGroup, pageContext: ContentPage, platformRestriction: PlatformData? = null) =
        wrapGroup(node, pageContext) { node.children.forEach { it.build(this, pageContext, platformRestriction) } }

    open fun T.wrapGroup(node: ContentGroup, pageContext: ContentPage, childrenCallback: T.() -> Unit) =
        childrenCallback()

    open fun T.buildLinkText(
        nodes: List<ContentNode>,
        pageContext: ContentPage,
        platformRestriction: PlatformData? = null
    ) {
        nodes.forEach { it.build(this, pageContext, platformRestriction) }
    }

    open fun T.buildCode(code: List<ContentNode>, language: String, pageContext: ContentPage) {
        code.forEach { it.build(this, pageContext) }
    }

    open fun T.buildHeader(node: ContentHeader, pageContext: ContentPage, platformRestriction: PlatformData? = null) {
        buildHeader(node.level) { node.children.forEach { it.build(this, pageContext, platformRestriction) } }
    }

    open fun ContentNode.build(builder: T, pageContext: ContentPage, platformRestriction: PlatformData? = null) =
        builder.buildContentNode(this, pageContext, platformRestriction)

    open fun T.buildContentNode(
        node: ContentNode,
        pageContext: ContentPage,
        platformRestriction: PlatformData? = null
    ) {
        if (platformRestriction == null || platformRestriction in node.platforms) {
            when (node) {
                is ContentText -> buildText(node)
                is ContentHeader -> buildHeader(node, pageContext, platformRestriction)
                is ContentCode -> buildCode(node.children, node.language, pageContext)
                is ContentDRILink ->
                    buildLink(locationProvider.resolve(node.address, node.platforms.toList(), pageContext)) {
                        buildLinkText(node.children, pageContext, platformRestriction)
                    }
                is ContentResolvedLink -> buildLink(node.address) {
                    buildLinkText(node.children, pageContext, platformRestriction)
                }
                is ContentEmbeddedResource -> buildResource(node, pageContext)
                is ContentList -> buildList(node, pageContext, platformRestriction)
                is ContentTable -> buildTable(node, pageContext, platformRestriction)
                is ContentGroup -> buildGroup(node, pageContext, platformRestriction)
                is ContentBreakLine -> buildNewLine()
                is PlatformHintedContent -> buildPlatformDependent(node, pageContext)
                else -> buildError(node)
            }
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
            context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(newRoot)

        root.children<ModulePageNode>().forEach { renderPackageList(it) }

        renderPages(newRoot)
    }
}

fun ContentPage.platforms() = this.content.platforms.toList()