package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.toDisplaySourceSets
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

class SourceLinksTransformer(val context: DokkaContext) : PageTransformer {

    private val builder : PageContentBuilder =  PageContentBuilder(
        context.plugin<DokkaBase>().querySingle { commentsToContentConverter },
        context.plugin<DokkaBase>().querySingle { signatureProvider },
        context.logger
    )

    override fun invoke(input: RootPageNode) =
        input.transformContentPagesTree { node ->
            when (node) {
                is WithDocumentables ->
                    node.documentables.filterIsInstance<WithSources>().flatMap { resolveSources(it) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { node.addSourcesContent(it) }
                    ?: node
                else -> node
            }
        }

    private fun getSourceLinks() = context.configuration.sourceSets
        .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it) } }

    private fun resolveSources(documentable: WithSources) = documentable.sources
        .mapNotNull { entry ->
            getSourceLinks().find { File(entry.value.path).startsWith(it.path) && it.sourceSetData == entry.key }?.let {
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
    ): ContentGroup {
        val documentables = (node as? WithDocumentables)?.documentables.orEmpty()
        return contentFor(
            node.dri,
            documentables.flatMap { it.sourceSets }.toSet()
        ) {
            header(2, "Sources", kind = ContentKind.Source)
            +ContentTable(
                header = emptyList(),
                children = sources.map {
                    buildGroup(node.dri, setOf(it.first), kind = ContentKind.Source, extra = mainExtra + SymbolAnchorHint(it.second, ContentKind.Source)) {
                        link("${it.first.displayName} source", it.second)
                    }
                },
                dci = DCI(node.dri, ContentKind.Source),
                sourceSets = documentables.flatMap { it.sourceSets }.toDisplaySourceSets(),
                style = emptySet(),
                extra = mainExtra + SimpleAttr.header("Sources")
            )
        }
    }

    private fun DocumentableSource.toLink(sourceLink: SourceLink): String {
        val sourcePath = File(this.path).canonicalPath.replace("\\", "/")
        val sourceLinkPath = File(sourceLink.path).canonicalPath.replace("\\", "/")

        val lineNumber = when (this) {
            is DescriptorDocumentableSource -> this.descriptor
                .cast<DeclarationDescriptorWithSource>()
                .source.getPsi()
                ?.lineNumber()
            is PsiDocumentableSource -> this.psi.lineNumber()
            else -> null
        }
        return sourceLink.url +
                sourcePath.split(sourceLinkPath)[1] +
                sourceLink.lineSuffix +
                "${lineNumber ?: 1}"
    }

    private fun ContentNode.addTable(table: ContentGroup): ContentNode =
        when (this) {
            is ContentGroup -> {
                if (hasTabbedContent()) {
                    copy(
                        children = children.map {
                            if (it.hasStyle(ContentStyle.TabbedContent) && it is ContentGroup) {
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
        sourceLinkDefinition.localDirectory,
        sourceLinkDefinition.remoteUrl.toExternalForm(),
        sourceLinkDefinition.remoteLineSuffix,
        sourceSetData
    )
}

fun ContentGroup.hasTabbedContent(): Boolean = children.any { it.hasStyle(ContentStyle.TabbedContent) }
