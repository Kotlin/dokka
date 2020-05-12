package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DescriptorDocumentableSource
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.WithExpectActual
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.sourceSet
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SourceLinksTransformer(val context: DokkaContext) : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode {

        val sourceLinks = context.configuration.passesConfigurations
            .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it.sourceSet) } }

        return input.transformContentPagesTree { node ->
            node.documentable.safeAs<WithExpectActual>()?.sources?.entries?.fold(node) { acc, entry ->
                sourceLinks.find { entry.value.path.contains(it.path) && it.sourceSetData == entry.key }?.run {
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

    private fun ContentNode.addSource(sourceSetData: SourceSetData, address: String?): ContentNode =
        if (address != null) when (this) {
            is ContentGroup -> copy(
                children = children + listOf(platformHintedContentResolvedLink(sourceSetData, dci.dri, address))
            )
            else -> ContentGroup(
                children = listOf(this, platformHintedContentResolvedLink(sourceSetData, dci.dri, address)),
                extra = this.extra,
                sourceSets = this.sourceSets,
                dci = this.dci,
                style = this.style
            )
        } else this

    private fun platformHintedContentResolvedLink(sourceSetData: SourceSetData, dri: Set<DRI>, address: String) =
        PlatformHintedContent(
            inner = ContentResolvedLink(
                children = listOf(
                    ContentText(
                        text = "(source)",
                        dci = DCI(dri, ContentKind.BriefComment),
                        sourceSets = setOf(sourceSetData),
                        style = emptySet(),
                        extra = PropertyContainer.empty()
                    )
                ),
                address = address,
                extra = PropertyContainer.empty(),
                dci = DCI(dri, ContentKind.Source),
                sourceSets = setOf(sourceSetData),
                style = emptySet()
            ),
            sourceSets = setOf(sourceSetData)
        )

    private fun PsiElement.lineNumber(): Int? {
        val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
        // IJ uses 0-based line-numbers; external source browsers use 1-based
        return doc?.getLineNumber(textRange.startOffset)?.plus(1)
    }
}

data class SourceLink(val path: String, val url: String, val lineSuffix: String?, val sourceSetData: SourceSetData) {
    constructor(sourceLinkDefinition: DokkaConfiguration.SourceLinkDefinition, sourceSetData: SourceSetData) : this(
        sourceLinkDefinition.path, sourceLinkDefinition.url, sourceLinkDefinition.lineSuffix, sourceSetData
    )
}