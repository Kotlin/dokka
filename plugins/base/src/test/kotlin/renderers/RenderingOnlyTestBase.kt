package renderers

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.DefaultLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.LocationProvider
import org.jetbrains.dokka.base.resolvers.LocationProviderFactory
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.testApi.context.MockContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import utils.TestOutputWriter

abstract class RenderingOnlyTestBase {
    val files = TestOutputWriter()
    val context = MockContext(
        DokkaBase().outputWriter to { _ -> files },
        DokkaBase().locationProviderFactory to ::DefaultLocationProviderFactory
    )

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


internal object EmptyCommentConverter : CommentsToContentConverter {
    override fun buildContent(
        docTag: DocTag,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style>,
        extras: Set<Extra>
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