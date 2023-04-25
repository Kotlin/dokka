package org.jetbrains.dokka.base.renderers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.model.DisplaySourceSet
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
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    )

    abstract fun T.buildLineBreak()
    open fun T.buildLineBreak(node: ContentBreakLine, pageContext: ContentPage) = buildLineBreak()

    abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage)
    abstract fun T.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    )

    abstract fun T.buildText(textNode: ContentText)
    abstract fun T.buildNavigation(page: PageNode)

    abstract fun buildPage(page: ContentPage, content: (T, ContentPage) -> Unit): String
    abstract fun buildError(node: ContentNode)

    open fun T.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = buildContentNode(content.inner, pageContext)

    open fun T.buildGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) =
        wrapGroup(node, pageContext) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }

    open fun T.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) =
        node.children.forEach { it.build(this, pageContext) }

    open fun T.wrapGroup(node: ContentGroup, pageContext: ContentPage, childrenCallback: T.() -> Unit) =
        childrenCallback()

    open fun T.buildText(
        nodes: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        nodes.forEach { it.build(this, pageContext, sourceSetRestriction) }
    }

    open fun T.buildCodeBlock(code: ContentCodeBlock, pageContext: ContentPage) {
        code.children.forEach { it.build(this, pageContext) }
    }

    open fun T.buildCodeInline(code: ContentCodeInline, pageContext: ContentPage) {
        code.children.forEach { it.build(this, pageContext) }
    }

    open fun T.buildHeader(
        node: ContentHeader,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        buildHeader(node.level, node) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }
    }

    open fun ContentNode.build(
        builder: T,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) =
        builder.buildContentNode(this, pageContext, sourceSetRestriction)

    open fun T.buildContentNode(
        node: ContentNode,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        if (sourceSetRestriction.isNullOrEmpty() || node.sourceSets.any { it in sourceSetRestriction }) {
            when (node) {
                is ContentText -> buildText(node)
                is ContentHeader -> buildHeader(node, pageContext, sourceSetRestriction)
                is ContentCodeBlock -> buildCodeBlock(node, pageContext)
                is ContentCodeInline -> buildCodeInline(node, pageContext)
                is ContentDRILink -> buildDRILink(node, pageContext, sourceSetRestriction)
                is ContentResolvedLink -> buildResolvedLink(node, pageContext, sourceSetRestriction)
                is ContentEmbeddedResource -> buildResource(node, pageContext)
                is ContentList -> buildList(node, pageContext, sourceSetRestriction)
                is ContentTable -> buildTable(node, pageContext, sourceSetRestriction)
                is ContentGroup -> buildGroup(node, pageContext, sourceSetRestriction)
                is ContentBreakLine -> buildLineBreak(node, pageContext)
                is PlatformHintedContent -> buildPlatformDependent(node, pageContext, sourceSetRestriction)
                is ContentDivergentGroup -> buildDivergent(node, pageContext)
                is ContentDivergentInstance -> buildDivergentInstance(node, pageContext)
                else -> buildError(node)
            }
        }
    }

    open fun T.buildDRILink(
        node: ContentDRILink,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        locationProvider.resolve(node.address, node.sourceSets, pageContext)?.let { address ->
            buildLink(address) {
                buildText(node.children, pageContext, sourceSetRestriction)
            }
        } ?: buildText(node.children, pageContext, sourceSetRestriction)
    }

    open fun T.buildResolvedLink(
        node: ContentResolvedLink,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildLink(node.address) {
            buildText(node.children, pageContext, sourceSetRestriction)
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
        val path by lazy {
            locationProvider.resolve(page, skipExtension = true)
                ?: throw DokkaException("Cannot resolve path for ${page.name}")
        }
        when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".html")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".html")
                is RenderingStrategy.DriLocationResolvableWrite -> outputWriter.write(
                    path,
                    strategy.contentToResolve { dri, sourcesets ->
                        locationProvider.resolve(dri, sourcesets)
                    },
                    ""
                )
                is RenderingStrategy.PageLocationResolvableWrite -> outputWriter.write(
                    path,
                    strategy.contentToResolve { pageToLocate, context ->
                        locationProvider.resolve(pageToLocate, context)
                    },
                    ""
                )
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
        beforeTransformer: (ContentDivergentInstance, ContentPage, DisplaySourceSet) -> String,
        afterTransformer: (ContentDivergentInstance, ContentPage, DisplaySourceSet) -> String
    ): Map<SerializedBeforeAndAfter, List<InstanceWithSource>> =
        children.flatMap { instance ->
            instance.sourceSets.map { sourceSet ->
                Pair(instance, sourceSet) to Pair(
                    beforeTransformer(instance, pageContext, sourceSet),
                    afterTransformer(instance, pageContext, sourceSet)
                )
            }
        }.groupBy(
            Pair<InstanceWithSource, SerializedBeforeAndAfter>::second,
            Pair<InstanceWithSource, SerializedBeforeAndAfter>::first
        )
}

internal typealias SerializedBeforeAndAfter = Pair<String, String>
internal typealias InstanceWithSource = Pair<ContentDivergentInstance, DisplaySourceSet>

fun ContentPage.sourceSets() = this.content.sourceSets
