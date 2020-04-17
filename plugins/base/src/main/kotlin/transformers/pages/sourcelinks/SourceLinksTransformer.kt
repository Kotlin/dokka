package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.DescriptorDocumentableSource
import org.jetbrains.dokka.model.WithExpectActual
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SourceLinksTransformer(val context: DokkaContext, val builder: PageContentBuilder) : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode {

        val sourceLinks = context.configuration.passesConfigurations
            .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it.platformData) } }

        return input.transformContentPagesTree { node ->
            node.documentable.safeAs<WithExpectActual>()?.sources?.map?.entries?.let { entries ->
                val resolvedSources = entries.mapNotNull { entry ->
                    sourceLinks.find { entry.value.path.contains(it.path) && it.platformData == entry.key }?.let {
                        Pair(
                            entry.key,
                            entry.value.cast<DescriptorDocumentableSource>().toLink(it)
                        )
                    }
                }
                if (resolvedSources.isNotEmpty()) {
                    val table = builder.contentFor(node.dri.first(), node.documentable!!.platformData.toSet()) {
                        header(2) { text("Sources") }
                        +ContentTable(
                            emptyList(),
                            resolvedSources.map {
                                buildGroup(node.dri.first(), setOf(it.first), ContentKind.Source) {
                                    platformDependentHint(node.dri.first(), setOf(it.first)) {
                                        +link("(source)", it.second, ContentKind.Source, mainPlatformData, mainStyles, mainExtra)
                                    }
                                }
                            },
                            DCI(node.dri, ContentKind.Subtypes),
                            node.documentable!!.platformData.toSet(),
                            style = emptySet()
                        )
                    }
                    node.modified(content = node.content.addTable(table))
                } else {
                    node
                }
            } ?: node
        }
    }

    private fun DescriptorDocumentableSource.toLink(sourceLink: SourceLink): String =
        sourceLink.url +
        this.path.split(sourceLink.path)[1] +
        sourceLink.lineSuffix +
        "${this.descriptor.cast<DeclarationDescriptorWithSource>().source.getPsi()?.lineNumber() ?: 1}"

    private fun ContentNode.addTable(table: ContentGroup): ContentNode =
        when (this) {
            is ContentGroup -> copy(
                children = children + table
            )
            else -> ContentGroup(
                children = listOf(this, table),
                extra = this.extra,
                platforms = this.platforms,
                dci = this.dci,
                style = this.style
            )
        }

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