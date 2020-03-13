package renderers

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.html.RootCreator
import org.jetbrains.dokka.base.resolvers.external.DokkaExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.testApi.context.MockContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import utils.TestOutputWriter

abstract class RenderingOnlyTestBase {
    val files = TestOutputWriter()
    val context = MockContext(
        DokkaBase().outputWriter to { _ -> files },
        DokkaBase().locationProviderFactory to ::DefaultLocationProviderFactory,
        DokkaBase().htmlPreprocessors to { _ -> RootCreator },
        DokkaBase().externalLocationProviderFactory to { _ -> ::JavadocExternalLocationProviderFactory },
        DokkaBase().externalLocationProviderFactory to { _ -> ::DokkaExternalLocationProviderFactory }
    )

    protected val renderedContent: Element by lazy {
        files.contents.getValue("test-page.html").let { Jsoup.parse(it) }.select("#content").single()
    }

    protected fun linesAfterContentTag() =
        files.contents.getValue("test-page.html").lines()
            .dropWhile { !it.contains("""<div id="content">""") }
            .joinToString(separator = "") { it.trim() }

}

class TestPage(callback: PageContentBuilder.DocumentableContentBuilder.() -> Unit): RootPageNode(), ContentPage {
    override val dri: Set<DRI> = setOf(DRI.topLevel)
    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()
    override val name: String
        get() = "testPage"
    override val children: List<PageNode>
        get() = emptyList()

    override val content: ContentNode = PageContentBuilder(
        EmptyCommentConverter,
        KotlinSignatureProvider(EmptyCommentConverter, DokkaConsoleLogger),
        DokkaConsoleLogger
    ).contentFor(
        DRI.topLevel,
        emptySet(),
        block = callback
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ) = this

    override fun modified(name: String, children: List<PageNode>) = this
}

fun Element.match(vararg matchers: Any): Unit =
    childNodes()
        .filter { it !is TextNode || it.text().isNotBlank() }
        .let { it.drop(it.size - matchers.size) }
        .zip(matchers)
        .forEach { (n, m) -> m.accepts(n) }

open class Tag(val name: String, vararg val matchers: Any)
class Div(vararg matchers: Any): Tag("div", *matchers)
class P(vararg matchers: Any): Tag("p", *matchers)

private fun Any.accepts(n: Node) {
    when (this) {
        is String -> assert(n is TextNode && n.text().trim() == this.trim()) { "\"$this\" expected but found: $n" }
        is Tag ->  {
            assert(n is Element && n.tagName() == name) { "Tag $name expected but found: $n" }
            if (n is Element && matchers.isNotEmpty()) n.match(*matchers)
        }
        else -> throw IllegalArgumentException("$this is not proper matcher")
    }
}


internal object EmptyCommentConverter : CommentsToContentConverter {
    override fun buildContent(
        docTag: DocTag,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style>,
        extras: PropertyContainer<ContentNode>
    ): List<ContentNode> = emptyList()
}

internal object EmptyLocationProviderFactory: LocationProviderFactory {
    override fun getLocationProvider(pageNode: RootPageNode) = object : LocationProvider {
        override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String = ""

        override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String = node.name

        override fun resolveRoot(node: PageNode): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun ancestors(node: PageNode): List<PageNode> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}