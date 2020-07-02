package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.model.WithExpectActual
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.cast

class SourceLinksTransformer(val context: DokkaContext, val builder: PageContentBuilder) : PageTransformer {

    override fun invoke(input: RootPageNode) =
        input.transformContentPagesTree { node ->
            when (val documentable = node.documentable) {
                is WithExpectActual -> resolveSources(documentable)
                    .takeIf { it.isNotEmpty() }
                    ?.let { node.addSourcesContent(it) }
                    ?: node
                else -> node
            }
        }

    private fun getSourceLinks() = context.configuration.sourceSets
        .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it) } }

    private fun resolveSources(documentable: WithExpectActual) = documentable.sources
        .mapNotNull { entry ->
            getSourceLinks().find { entry.value.path.contains(it.path) && it.sourceSetData == entry.key }?.let {
                Pair(
                    entry.key,
                    entry.value.toLink(it)
                )
            }
        }

    private fun ContentPage.addSourcesContent(sources: List<Pair<DokkaSourceSet, String>>) = builder
        .buildSourcesContent(this, sources)
        .let {
            this.modified(
                content = this.content.addTable(it)
            )
        }

    private fun PageContentBuilder.buildSourcesContent(
        node: ContentPage,
        sources: List<Pair<DokkaSourceSet, String>>
    ) = contentFor(
        node.dri.first(),
        node.documentable!!.sourceSets.toSet()
    ) {
        header(2, "Sources")
        +ContentTable(
            emptyList(),
            sources.map {
                buildGroup(node.dri, setOf(it.first), kind = ContentKind.Source) {
                    link("(source)", it.second)
                }
            },
            DCI(node.dri, ContentKind.Source),
            node.documentable!!.sourceSets.toSet(),
            style = emptySet(),
            extra = mainExtra + SimpleAttr.header("Sources")
        )
    }

    private fun DocumentableSource.toLink(sourceLink: SourceLink): String {
        val lineNumber = when (this) {
            is DescriptorDocumentableSource -> this.descriptor
                .cast<DeclarationDescriptorWithSource>()
                .source.getPsi()
                ?.lineNumber()
            is PsiDocumentableSource -> this.psi.lineNumber()
            else -> null
        }
        return sourceLink.url +
                this.path.split(sourceLink.path)[1] +
                sourceLink.lineSuffix +
                "${lineNumber ?: 1}"
    }

    private fun ContentNode.addTable(table: ContentGroup): ContentNode =
        when (this) {
            is ContentGroup -> {
                if(hasTabbedContent()){
                    copy(
                        children = children.map {
                            if(it.hasStyle(ContentStyle.TabbedContent) && it is ContentGroup){
                                it.copy(children = it.children + table)
                            } else {
                                it
                            }
                        }
                    )
                } else {
                    copy(children = children + table)
                }

            }
            else -> ContentGroup(
                children = listOf(this, table),
                extra = this.extra,
                sourceSets = this.sourceSets,
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

data class SourceLink(val path: String, val url: String, val lineSuffix: String?, val sourceSetData: DokkaSourceSet) {
    constructor(sourceLinkDefinition: DokkaConfiguration.SourceLinkDefinition, sourceSetData: DokkaSourceSet) : this(
        sourceLinkDefinition.path, sourceLinkDefinition.url, sourceLinkDefinition.lineSuffix, sourceSetData
    )
}

fun ContentGroup.hasTabbedContent(): Boolean = children.any { it.hasStyle(ContentStyle.TabbedContent) }