/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages

import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet

/**
 * It works ONLY with a content model from the base plugin.
 */
internal class DefaultSamplesTransformer(val context: DokkaContext) : PageTransformer {

    private val sampleAnalysisEnvironment: SampleAnalysisEnvironmentCreator =
        context.plugin<KotlinAnalysisPlugin>().querySingle { sampleAnalysisEnvironmentCreator }

    override fun invoke(input: RootPageNode): RootPageNode {
        return sampleAnalysisEnvironment.use {
            input.transformContentPagesTree { page ->
                val samples = (page as? WithDocumentables)?.documentables?.flatMap {
                    it.documentation.entries.flatMap { entry ->
                        entry.value.children.filterIsInstance<Sample>().map { entry.key to it }
                    }
                }?.takeIf { it.isNotEmpty() } ?: return@transformContentPagesTree page

                val newContent = samples.fold(page.content) { acc, (sampleSourceSet, sample) ->
                    resolveSample(sampleSourceSet, sample.name)
                        ?.let {
                            acc.addSample(page, sample.name, it)
                        } ?: acc
                }

                page.modified(
                    content = newContent
                )
            }
        }
    }

    private fun ContentNode.addSample(
        contentPage: ContentPage,
        fqLink: String,
        sample: SampleSnippet,
    ): ContentNode {
        val node = contentCode(contentPage.content.sourceSets, contentPage.dri, sample, "kotlin")
        return dfs(fqLink, node)
    }

    private fun ContentNode.dfs(fqName: String, node: ContentCodeBlock): ContentNode {
        return when (this) {
            is ContentHeader -> copy(children.map { it.dfs(fqName, node) })
            is ContentDivergentGroup -> @Suppress("UNCHECKED_CAST") copy(children.map {
                it.dfs(fqName, node)
            } as List<ContentDivergentInstance>)

            is ContentDivergentInstance -> copy(
                before.let { it?.dfs(fqName, node) },
                divergent.dfs(fqName, node),
                after.let { it?.dfs(fqName, node) })

            is ContentCodeBlock -> copy(children.map { it.dfs(fqName, node) })
            is ContentCodeInline -> copy(children.map { it.dfs(fqName, node) })
            is ContentDRILink -> copy(children.map { it.dfs(fqName, node) })
            is ContentResolvedLink -> copy(children.map { it.dfs(fqName, node) })
            is ContentEmbeddedResource -> copy(children.map { it.dfs(fqName, node) })
            is ContentTable -> copy(children = children.map { it.dfs(fqName, node) as ContentGroup })
            is ContentList -> copy(children.map { it.dfs(fqName, node) })
            is ContentGroup -> copy(children.map { it.dfs(fqName, node) })
            is PlatformHintedContent -> copy(inner.dfs(fqName, node))
            is ContentText -> if (text == fqName) node else this
            is ContentBreakLine -> this
            else -> this.also { context.logger.error("Could not recognize $this ContentNode in SamplesTransformer") }
        }
    }

    private fun contentCode(
        sourceSets: Set<DisplaySourceSet>,
        dri: Set<DRI>,
        sample: SampleSnippet,
        language: String,
        styles: Set<Style> = emptySet(),
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
    ): ContentCodeBlock {
        val dci = DCI(dri, ContentKind.Sample)
        val children = buildContentNodesFromBody(sample, dci, sourceSets)
        return ContentCodeBlock(
            children = children,
            language = language,
            dci = dci,
            sourceSets = sourceSets,
            style = styles + TextStyle.Monospace,
            extra = extra
        )
    }

    /**
     * Parses the sample body for link placeholders of the form `%index%content%index%`
     * and replaces them with [ContentDRILink] nodes pointing to the corresponding [DRI]
     * from [SampleSnippet.links].
     */
    private fun buildContentNodesFromBody(
        sample: SampleSnippet,
        dci: DCI,
        sourceSets: Set<DisplaySourceSet>,
    ): List<ContentNode> {
        if (sample.links.isEmpty()) {
            return listOf(
                ContentText(
                    text = sample.body,
                    dci = dci,
                    sourceSets = sourceSets,
                    style = emptySet(),
                    extra = PropertyContainer.empty()
                )
            )
        }

        val result = mutableListOf<ContentNode>()
        // Pattern: %<index>%<content>%<index>% where <index> is a 0-based index into sample.links
        val linkPattern = Regex("""%(\d+)%(.*?)%\1%""")
        var lastEnd = 0

        for (match in linkPattern.findAll(sample.body)) {
            // Add text before this link
            if (match.range.first > lastEnd) {
                result.add(
                    ContentText(
                        text = sample.body.substring(lastEnd, match.range.first),
                        dci = dci,
                        sourceSets = sourceSets,
                        style = emptySet(),
                        extra = PropertyContainer.empty()
                    )
                )
            }

            val linkIndex = match.groupValues[1].toInt()
            val linkText = match.groupValues[2]
            val linkDri = sample.links.getOrNull(linkIndex)

            if (linkDri != null) {
                result.add(
                    ContentDRILink(
                        children = listOf(
                            ContentText(
                                text = linkText,
                                dci = dci,
                                sourceSets = sourceSets,
                                style = emptySet(),
                                extra = PropertyContainer.empty()
                            )
                        ),
                        address = linkDri,
                        dci = dci,
                        sourceSets = sourceSets,
                    )
                )
            } else {
                // Invalid link index — render as plain text
                result.add(
                    ContentText(
                        text = linkText,
                        dci = dci,
                        sourceSets = sourceSets,
                        style = emptySet(),
                        extra = PropertyContainer.empty()
                    )
                )
            }

            lastEnd = match.range.last + 1
        }

        // Add remaining text after the last link
        if (lastEnd < sample.body.length) {
            result.add(
                ContentText(
                    text = sample.body.substring(lastEnd),
                    dci = dci,
                    sourceSets = sourceSets,
                    style = emptySet(),
                    extra = PropertyContainer.empty()
                )
            )
        }

        return result
    }
}
