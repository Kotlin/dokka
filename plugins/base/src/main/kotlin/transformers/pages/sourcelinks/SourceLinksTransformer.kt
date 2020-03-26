package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DescriptorDocumentableSource
import org.jetbrains.dokka.model.WithExpectActual
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SourceLinksTransformer(val context: DokkaContext) : PageTransformer {

    private lateinit var sourceLinks: List<SourceLink>

    override fun invoke(input: RootPageNode): RootPageNode {

        sourceLinks = context.configuration.passesConfigurations
            .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it.platformData) } }

        return input.transformContentPagesTree { node ->
            node.documentable.safeAs<WithExpectActual>()?.sources?.map?.entries?.fold(node) { acc, entry ->
                sourceLinks.find { entry.value.path.contains(it.path) && it.platformData == entry.key }?.run {
                    acc.modified(
                        content = acc.content.addSource(
                            entry.key,
                            entry.value.cast<DescriptorDocumentableSource>().toLink(this)
                        )
                    )
                } ?: acc
            } ?: node
        }
    }

    private fun DescriptorDocumentableSource.toLink(sourceLink: SourceLink): String =
        sourceLink.url +
        this.path.split(sourceLink.path)[1] +
        sourceLink.lineSuffix +
        "${this.descriptor.cast<DeclarationDescriptorWithSource>().source.getPsi()?.lineNumber() ?: 1}"

    private fun ContentNode.addSource(platformData: PlatformData, address: String?): ContentNode =
        if (address != null) when (this) {
            is ContentGroup -> copy(
                children = children + listOf(platformHintedContentResolvedLink(platformData, dci.dri, address))
            )
            else -> ContentGroup(
                children = listOf(this, platformHintedContentResolvedLink(platformData, dci.dri, address)),
                extra = this.extra,
                platforms = this.platforms,
                dci = this.dci,
                style = this.style
            )
        } else this

    private fun platformHintedContentResolvedLink(platformData: PlatformData, dri: Set<DRI>, address: String) =
        PlatformHintedContent(
            inner = ContentResolvedLink(
                children = listOf(
                    ContentText(
                        text = "(source)",
                        dci = DCI(dri, ContentKind.BriefComment),
                        platforms = setOf(platformData),
                        style = emptySet(),
                        extra = PropertyContainer.empty()
                    )
                ),
                address = address,
                extra = PropertyContainer.empty(),
                dci = DCI(dri, ContentKind.Source),
                platforms = setOf(platformData),
                style = emptySet()
            ),
            platforms = setOf(platformData)
        )

    private fun PsiElement.lineNumber(): Int? {
        val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
        // IJ uses 0-based line-numbers; external source browsers use 1-based
        return doc?.getLineNumber(textRange.startOffset)?.plus(1)
    }
}

data class SourceLink(val path: String, val url: String, val lineSuffix: String?, val platformData: PlatformData) {
    constructor(sourceLinkDefinition: DokkaConfiguration.SourceLinkDefinition, platformData: PlatformData) : this(
        sourceLinkDefinition.path, sourceLinkDefinition.url, sourceLinkDefinition.lineSuffix, platformData
    )
}