package org.jetbrains.dokka.base.renderers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
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

    abstract fun T.buildHeader(level: Int, node: ContentHeader, content: T.() -> Unit)
    abstract fun T.buildLink(address: String, content: T.() -> Unit)
    abstract fun T.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    )

    abstract fun T.buildNewLine()
    abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage)
    abstract fun T.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    )

    abstract fun T.buildText(textNode: ContentText)
    abstract fun T.buildNavigation(page: PageNode)

    abstract fun buildPage(page: ContentPage, content: (T, ContentPage) -> Unit): String
    abstract fun buildError(node: ContentNode)

    open fun T.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) = buildContentNode(content.inner, pageContext)

    open fun T.buildGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    ) =
        wrapGroup(node, pageContext) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }

    open fun T.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) =
        node.children.forEach { it.build(this, pageContext) }

    open fun T.wrapGroup(node: ContentGroup, pageContext: ContentPage, childrenCallback: T.() -> Unit) =
        childrenCallback()

    open fun T.buildLinkText(
        nodes: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    ) {
        nodes.forEach { it.build(this, pageContext, sourceSetRestriction) }
    }

    open fun T.buildCode(code: ContentCode, pageContext: ContentPage) {
        code.children.forEach { it.build(this, pageContext) }
    }

    open fun T.buildHeader(
        node: ContentHeader,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    ) {
        buildHeader(node.level, node) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }
    }

    open fun ContentNode.build(
        builder: T,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    ) =
        builder.buildContentNode(this, pageContext, sourceSetRestriction)

    open fun T.buildContentNode(
        node: ContentNode,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>? = null
    ) {
        if (sourceSetRestriction == null || node.sourceSets.any { it in sourceSetRestriction }) {
            when (node) {
                is ContentText -> buildText(node)
                is ContentHeader -> buildHeader(node, pageContext, sourceSetRestriction)
                is ContentCode -> buildCode(node, pageContext)
                is ContentDRILink ->
                    buildLink(locationProvider.resolve(node.address, node.sourceSets, pageContext)) {
                        buildLinkText(node.children, pageContext, sourceSetRestriction)
                    }
                is ContentResolvedLink -> buildLink(node.address) {
                    buildLinkText(node.children, pageContext, sourceSetRestriction)
                }
                is ContentEmbeddedResource -> buildResource(node, pageContext)
                is ContentList -> buildList(node, pageContext, sourceSetRestriction)
                is ContentTable -> buildTable(node, pageContext, sourceSetRestriction)
                is ContentGroup -> buildGroup(node, pageContext, sourceSetRestriction)
                is ContentBreakLine -> buildNewLine()
                is PlatformHintedContent -> buildPlatformDependent(node, pageContext, sourceSetRestriction)
                is ContentDivergentGroup -> buildDivergent(node, pageContext)
                is ContentDivergentInstance -> buildDivergentInstance(node, pageContext)
                else -> buildError(node)
            }
        }
    }

    open fun T.buildDivergentInstance(node: ContentDivergentInstance, pageContext: ContentPage) {
        node.before?.build(this, pageContext)
        node.divergent.build(this, pageContext)
        node.after?.build(this, pageContext)
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

    protected fun ContentDivergentGroup.groupDivergentInstances(
        pageContext: ContentPage,
        beforeTransformer: (ContentDivergentInstance, ContentPage, DokkaSourceSet) -> String,
        afterTransformer: (ContentDivergentInstance, ContentPage, DokkaSourceSet) -> String
    ): Map<Pair<String, String>, List<Pair<ContentDivergentInstance, DokkaSourceSet>>> =
        children.flatMap { instance ->
            instance.sourceSets.map { sourceSet ->
                Pair(instance, sourceSet) to Pair(
                    beforeTransformer(instance, pageContext, sourceSet),
                    afterTransformer(instance, pageContext, sourceSet)
                )
            }
        }.groupBy(
            Pair<Pair<ContentDivergentInstance, DokkaSourceSet>, Pair<String, String>>::second,
            Pair<Pair<ContentDivergentInstance, DokkaSourceSet>, Pair<String, String>>::first
        )
}

fun ContentPage.sourceSets() = this.content.sourceSets