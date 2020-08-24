package renderers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter

class TestPage(callback: PageContentBuilder.DocumentableContentBuilder.() -> Unit) : RootPageNode(), ContentPage {
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
        sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
        styles: Set<Style>,
        extras: PropertyContainer<ContentNode>
    ): List<ContentNode> = emptyList()
}