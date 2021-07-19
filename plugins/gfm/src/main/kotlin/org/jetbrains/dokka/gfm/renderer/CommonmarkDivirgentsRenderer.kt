package org.jetbrains.dokka.gfm.renderer

import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext

abstract class CommonmarkDivirgentsRenderer(
    context: DokkaContext
) : CommonmarkInlinesRenderer(context) {

    override fun StringBuilder.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildPlatformDependentItem(content.inner, content.sourceSets, pageContext)
    }

    private fun StringBuilder.buildPlatformDependentItem(
        content: ContentNode,
        sourceSets: Set<DisplaySourceSet>,
        pageContext: ContentPage,
    ) {
        if (content is ContentGroup && content.children.firstOrNull { it is ContentTable } != null) {
            buildContentNode(content, pageContext, sourceSets)
        } else {
            val distinct = sourceSets.map {
                it to buildString { buildContentNode(content, pageContext, setOf(it)) }
            }.groupBy(Pair<DisplaySourceSet, String>::second, Pair<DisplaySourceSet, String>::first)

            distinct.filter { it.key.isNotBlank() }.forEach { (text, platforms) ->
                buildParagraph()
                buildSourceSetTags(platforms.toSet())
                buildLineBreak()
                append(text.trim())
                buildParagraph()
            }
        }
    }

    override fun StringBuilder.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {

        val distinct =
            node.groupDivergentInstances(pageContext, { instance, contentPage, sourceSet ->
                instance.before?.let { before ->
                    buildString { buildContentNode(before, pageContext, sourceSet) }
                } ?: ""
            }, { instance, contentPage, sourceSet ->
                instance.after?.let { after ->
                    buildString { buildContentNode(after, pageContext, sourceSet) }
                } ?: ""
            })

        distinct.values.forEach { entry ->
            val (instance, sourceSets) = entry.getInstanceAndSourceSets()

            buildParagraph()
            buildSourceSetTags(sourceSets)
            buildLineBreak()

            instance.before?.let {
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildParagraph()
            }

            entry.groupBy { buildString { buildContentNode(it.first.divergent, pageContext, setOf(it.second)) } }
                .values.forEach { innerEntry ->
                    val (innerInstance, innerSourceSets) = innerEntry.getInstanceAndSourceSets()
                    if (sourceSets.size > 1) {
                        buildSourceSetTags(innerSourceSets)
                        buildLineBreak()
                    }
                    innerInstance.divergent.build(
                        this@buildDivergent,
                        pageContext,
                        setOf(innerSourceSets.first())
                    ) // It's workaround to render content only once
                    buildParagraph()
                }

            instance.after?.let {
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
            }

            buildParagraph()
        }
    }

    private fun List<Pair<ContentDivergentInstance, DisplaySourceSet>>.getInstanceAndSourceSets() =
        this.let { Pair(it.first().first, it.map { it.second }.toSet()) }

    private fun StringBuilder.buildSourceSetTags(sourceSets: Set<DisplaySourceSet>) =
        append(sourceSets.joinToString(prefix = "[", postfix = "]") { it.name })
}
