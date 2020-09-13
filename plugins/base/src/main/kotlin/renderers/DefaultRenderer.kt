package org.jetbrains.dokka.base.renderers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.resolveOrThrow
import org.jetbrains.dokka.base.transformers.pages.serialization.*
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.io.File

abstract class DefaultRenderer<T>(
    protected val context: DokkaContext
) : Renderer {

    protected val outputWriter = context.plugin<DokkaBase>().querySingle { outputWriter }

    protected lateinit var locationProvider: LocationProvider
        private set

    protected open val preprocessors: Iterable<PageTransformer> = emptyList()

    abstract fun T.buildHeader(level: Int, node: HeaderView, content: T.() -> Unit)
    abstract fun T.buildLink(address: String, content: T.() -> Unit)
    abstract fun T.buildList(
        node: ListView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    )

    abstract fun T.buildNewLine()
    abstract fun T.buildResource(node: ContentEmbeddedResource, pageContext: PagesSerializationView)
    abstract fun T.buildTable(
        node: TableView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    )

    abstract fun T.buildText(textNode: TextView)
    abstract fun T.buildNavigation(page: PageNode)

    abstract fun buildPage(page: PagesSerializationView, content: (T, PagesSerializationView) -> Unit): String
    abstract fun buildError(node: Content)

    open fun T.buildPlatformDependent(
        content: PlatformHintedContentView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = buildContentNode(content.inner, pageContext)

    open fun T.buildGroup(
        node: GroupView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) =
        wrapGroup(node, pageContext) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }

    open fun T.buildDivergent(node: DivergentGroupView, pageContext: PagesSerializationView) =
        node.children.forEach { it.build(this, pageContext) }

    open fun T.wrapGroup(node: GroupView, pageContext: PagesSerializationView, childrenCallback: T.() -> Unit) =
        childrenCallback()

    open fun T.buildText(
        nodes: List<Content>,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        nodes.forEach { it.build(this, pageContext, sourceSetRestriction) }
    }

    open fun T.buildCodeBlock(code: CodeView, pageContext: PagesSerializationView) {
        code.children.forEach { it.build(this, pageContext) }
    }

    open fun T.buildCodeInline(code: CodeView, pageContext: PagesSerializationView) {
        code.children.forEach { it.build(this, pageContext) }
    }

    open fun T.buildHeader(
        node: HeaderView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        buildHeader(node.level, node) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }
    }

    open fun Content.build(
        builder: T,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) =
        builder.buildContentNode(this, pageContext, sourceSetRestriction)

    open fun T.buildContentNode(
        node: Content,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        if (sourceSetRestriction == null || node.sourceSets.any { it in sourceSetRestriction }) {
            when (node) {
                is TextView -> buildText(node)
                is HeaderView -> buildHeader(node, pageContext, sourceSetRestriction)
                is CodeView -> buildCodeBlock(node, pageContext)
                is UnresolvedLinkView -> buildDRILink(node, pageContext, sourceSetRestriction)
                is ResolvedLinkView -> buildResolvedLink(node, pageContext, sourceSetRestriction)
                is ListView -> buildList(node, pageContext, sourceSetRestriction)
                is TableView -> buildTable(node, pageContext, sourceSetRestriction)
                is GroupView -> buildGroup(node, pageContext, sourceSetRestriction)
                is BreakLineView -> buildNewLine()
                is PlatformHintedContentView -> buildPlatformDependent(node, pageContext, sourceSetRestriction)
                is DivergentGroupView -> buildDivergent(node, pageContext)
                is DivergentInstanceView -> buildDivergentInstance(node, pageContext)
                else -> buildError(node)
            }
        }
    }

    open fun T.buildDRILink(
        node: UnresolvedLinkView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        locationProvider.resolve(node.address, node.sourceSets, pageContext)?.let { address ->
            buildLink(address) {
                buildText(node.children, pageContext, sourceSetRestriction)
            }
        } ?: buildText(node.children, pageContext, sourceSetRestriction)
    }

    open fun T.buildResolvedLink(
        node: ResolvedLinkView,
        pageContext: PagesSerializationView,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildLink(node.address) {
            buildText(node.children, pageContext, sourceSetRestriction)
        }
    }

    open fun T.buildDivergentInstance(node: DivergentInstanceView, pageContext: PagesSerializationView) {
        node.before?.build(this, pageContext)
        node.divergent.build(this, pageContext)
        node.after?.build(this, pageContext)
    }

    open fun buildPageContent(context: T, page: PagesSerializationView) {
        context.buildNavigation(page)
        page.content.build(context, page)
    }

    open suspend fun renderPage(page: PageNode) {
        val path by lazy {
            locationProvider.resolve(page, skipExtension = true)
                ?: throw DokkaException("Cannot resolve path for ${page.name}")
        }
        when (page) {
            is PagesSerializationView -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".html")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".html")
                is RenderingStrategy.LocationResolvableWrite -> outputWriter.write(path, strategy.contentToResolve { dri, sourcesets ->
                    locationProvider.resolveOrThrow(dri, sourcesets)
                }, "")
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

        val serialized = PagesSerializationTransformer(ContentSerializationTransformer()).invoke(newRoot)
        locationProvider =
            context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(serialized)

        runBlocking(Dispatchers.Default) {
            renderPages(serialized)
        }
    }

    protected fun DivergentGroupView.groupDivergentInstances(
        pageContext: PagesSerializationView,
        beforeTransformer: (DivergentInstanceView, PagesSerializationView, DisplaySourceSet) -> String,
        afterTransformer: (DivergentInstanceView, PagesSerializationView, DisplaySourceSet) -> String
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
internal typealias InstanceWithSource = Pair<DivergentInstanceView, DisplaySourceSet>

fun PagesSerializationView.sourceSets() = this.content.sourceSets
fun ContentPage.sourceSets() = this.content.sourceSets

fun ContentEmbeddedResource.isImage(): Boolean {
    val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "tif", "webp", "svg")
    return File(address).extension.toLowerCase() in imageExtensions
}