package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.ContentSourceSet
import org.jetbrains.dokka.model.toContentSourceSets
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

class SourceSetMergingPageTransformer(context: DokkaContext) : PageTransformer {

    private val mergedSourceSets = context.configuration.sourceSets.toContentSourceSets()
        .associateBy { sourceSet -> sourceSet.key }

    override fun invoke(input: RootPageNode): RootPageNode {
        return input.transformContentPagesTree { contentPage ->
            val content: ContentNode = contentPage.content
            val newContent = transformWithMergedSourceSets(content)
            contentPage.modified(content = newContent)
        }
    }

    private fun transformWithMergedSourceSets(
        contentNode: ContentNode
    ): ContentNode {
        val mergedSourceSets = contentNode.sourceSets.map { mergedSourceSets.getValue(it.key) }.toSet()
        return when (contentNode) {
            is ContentComposite -> contentNode
                .transformChildren(::transformWithMergedSourceSets)
                .withSourceSets(mergedSourceSets)
            else -> contentNode.withSourceSets(mergedSourceSets.toSet())
        }
    }
}

private val ContentSourceSet.key get() = SourceSetMergingKey(displayName, analysisPlatform)

private data class SourceSetMergingKey(private val displayName: String, private val platform: Platform)
