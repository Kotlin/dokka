/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.io.File

public class SourceLinksTransformer(
    public val context: DokkaContext
) : PageTransformer {

    private val builder : PageContentBuilder = PageContentBuilder(
        context.plugin<DokkaBase>().querySingle { commentsToContentConverter },
        context.plugin<DokkaBase>().querySingle { signatureProvider },
        context.logger
    )

    override fun invoke(input: RootPageNode): RootPageNode {
        val sourceLinks = getSourceLinksFromConfiguration()
        if (sourceLinks.isEmpty()) {
            return input
        }
       return input.transformContentPagesTree { node ->
            when (node) {
                is WithDocumentables -> {
                    val sources = node.documentables
                        .filterIsInstance<WithSources>()
                        .fold(mutableMapOf<DRI, List<Pair<DokkaSourceSet, String>>>()) { acc, documentable ->
                            val dri = (documentable as Documentable).dri
                            acc.compute(dri) { _, v ->
                                val sources = resolveSources(sourceLinks, documentable)
                                v?.plus(sources) ?: sources
                            }
                            acc
                        }
                    if (sources.isNotEmpty())
                        node.modified(content = transformContent(node.content, sources))
                    else
                        node
                }
                else -> node
            }
        }
    }

    private fun getSourceLinksFromConfiguration(): List<SourceLink> {
        return context.configuration.sourceSets
            .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it) } }
    }

    private fun resolveSources(
        sourceLinks: List<SourceLink>, documentable: WithSources
    ): List<Pair<DokkaSourceSet, String>> {
        return documentable.sources.mapNotNull { (sourceSet, documentableSource) ->
            val sourceLink = sourceLinks.find { sourceLink ->
                File(documentableSource.path).startsWith(sourceLink.path) && sourceLink.sourceSetData == sourceSet
            } ?: return@mapNotNull null

            sourceSet to documentableSource.toLink(sourceLink)
        }
    }

    private fun DocumentableSource.toLink(sourceLink: SourceLink): String {
        val sourcePath = File(this.path).invariantSeparatorsPath
        val sourceLinkPath = File(sourceLink.path).invariantSeparatorsPath

        val lineNumber = this.computeLineNumber()
        return sourceLink.url +
                sourcePath.split(sourceLinkPath)[1] +
                sourceLink.lineSuffix +
                "${lineNumber ?: 1}"
    }

    private fun ContentNode.signatureGroupOrNull() =
        (this as? ContentGroup)?.takeIf { it.dci.kind == ContentKind.Symbol }

    private fun transformContent(
        contentNode: ContentNode, sources: Map<DRI, List<Pair<DokkaSourceSet, String>>>
    ): ContentNode =
        contentNode.signatureGroupOrNull()?.let { sg ->
            val sgIds = sg.sourceSets.computeSourceSetIds()
            sources[sg.dci.dri.singleOrNull()]?.let { sourceLinks ->
                sourceLinks
                    .filter { it.first.sourceSetID in sgIds }
                    .takeIf { it.isNotEmpty() }
                    ?.let { filteredSourcesLinks ->
                        sg.copy(children = sg.children + filteredSourcesLinks.map {
                            buildContentLink(
                                sg.dci.dri.first(),
                                it.first,
                                it.second
                            )
                        })
                    }
            }
        } ?: when (contentNode) {
            is ContentComposite -> contentNode.transformChildren { transformContent(it, sources) }
            else -> contentNode
        }

    private fun buildContentLink(dri: DRI, sourceSet: DokkaSourceSet, link: String) = builder.contentFor(
        dri,
        setOf(sourceSet),
        ContentKind.Source,
        setOf(TextStyle.FloatingRight)
    ) {
        text("(")
        link("source", link)
        text(")")
    }
}

public data class SourceLink(
    val path: String,
    val url: String,
    val lineSuffix: String?,
    val sourceSetData: DokkaSourceSet
) {
    public constructor(
        sourceLinkDefinition: DokkaConfiguration.SourceLinkDefinition,
        sourceSetData: DokkaSourceSet
    ) : this(
        sourceLinkDefinition.localDirectory,
        sourceLinkDefinition.remoteUrl.toExternalForm(),
        sourceLinkDefinition.remoteLineSuffix,
        sourceSetData
    )
}
