/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.toDisplaySourceSets
import org.jetbrains.dokka.pages.ContentComposite
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class SourceSetMergingPageTransformer(context: DokkaContext) : PageTransformer {

    private val mergedSourceSets = context.configuration.sourceSets.toDisplaySourceSets()
        .associateBy { sourceSet -> sourceSet.key }

    override fun invoke(input: RootPageNode): RootPageNode {
        return input.transformContentPagesTree { contentPage ->
            val content: ContentNode = contentPage.content
            contentPage.modified(content = transformWithMergedSourceSets(content))
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

private val DisplaySourceSet.key get() = SourceSetMergingKey(name, platform)

private data class SourceSetMergingKey(private val displayName: String, private val platform: Platform)
