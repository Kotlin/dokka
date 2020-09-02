package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.toDisplaySourceSets
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaModuleContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

class SourceSetMergingPageTransformer : PageTransformer {

    override fun invoke(input: RootPageNode, context: DokkaModuleContext): RootPageNode {
        val mergedSourceSets = context.configuration.sourceSets.toDisplaySourceSets()
            .associateBy { sourceSet -> sourceSet.key }

        fun transformWithMergedSourceSets(
            contentNode: ContentNode
        ): ContentNode {
            val merged = contentNode.sourceSets.map { mergedSourceSets.getValue(it.key) }.toSet()
            return when (contentNode) {
                is ContentComposite -> contentNode
                    .transformChildren(::transformWithMergedSourceSets)
                    .withSourceSets(merged)
                else -> contentNode.withSourceSets(merged.toSet())
            }
        }

        return input.transformContentPagesTree { contentPage ->
            val content: ContentNode = contentPage.content
            contentPage.modified(content = transformWithMergedSourceSets(content))
        }
    }
}

private val DisplaySourceSet.key get() = SourceSetMergingKey(name, platform)

private data class SourceSetMergingKey(private val displayName: String, private val platform: Platform)
