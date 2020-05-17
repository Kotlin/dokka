package org.jetbrains.dokka.base.renderers

import kotlinx.coroutines.*
import kotlinx.html.FlowContent
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.model.SourceSetData
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
    abstract fun T.buildList(
        node: ContentList,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    )

    abstract fun T.buildNewLine()
    abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage)
    abstract fun T.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    )

    abstract fun T.buildText(textNode: ContentText)
    abstract fun T.buildNavigation(page: PageNode)

    abstract fun buildPage(page: ContentPage, content: (T, ContentPage) -> Unit): String
    abstract fun buildError(node: ContentNode)

    open fun T.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) =
        buildContentNode(content.inner, pageContext)

    open fun T.buildGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    ) =
        wrapGroup(node, pageContext) { node.children.forEach { it.build(this, pageContext, platformRestriction) } }

    open fun T.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) =
         node.children.forEach { it.build(this, pageContext) }

    open fun T.wrapGroup(node: ContentGroup, pageContext: ContentPage, childrenCallback: T.() -> Unit) =
        childrenCallback()

    open fun T.buildLinkText(
        nodes: List<ContentNode>,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    ) {
        nodes.forEach { it.build(this, pageContext, platformRestriction) }
    }

    open fun T.buildCode(code: List<ContentNode>, language: String, pageContext: ContentPage) {
        code.forEach { it.build(this, pageContext) }
    }

    open fun T.buildHeader(
        node: ContentHeader,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    ) {
        buildHeader(node.level) { node.children.forEach { it.build(this, pageContext, platformRestriction) } }
    }

    open fun ContentNode.build(
        builder: T,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    ) =
        builder.buildContentNode(this, pageContext, platformRestriction)

    open fun T.buildContentNode(
        node: ContentNode,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>? = null
    ) {
        if (platformRestriction == null || node.sourceSets.any { it in platformRestriction }  ) {
            when (node) {
                is ContentText -> buildText(node)
                is ContentHeader -> buildHeader(node, pageContext, platformRestriction)
                is ContentCode -> buildCode(node.children, node.language, pageContext)
                is ContentDRILink ->
                    buildLink(locationProvider.resolve(node.address, node.sourceSets.toList(), pageContext)) {
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
                is ContentDivergentGroup -> buildDivergent(node, pageContext)
                else -> buildError(node)
            }
        }
    }

    open fun buildPageContent(context: T, page: ContentPage) {
        context.buildNavigation(page)
        page.content.build(context, page)
    }

    open suspend fun renderPage(page: PageNode) {
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

    private suspend fun renderPages(root: PageNode) {
        coroutineScope {
            renderPage(root)

            root.children.forEach {
                launch { renderPages(it) }
            }
        }
    }

    override fun render(root: RootPageNode) {
        val newRoot = preprocessors.fold(root) { acc, t -> t(acc) }

        locationProvider =
            context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(newRoot)

        runBlocking(Dispatchers.Default) {
            renderPages(newRoot)
        }
    }
}

fun ContentPage.platforms() = this.content.sourceSets.toList()