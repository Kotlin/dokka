package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.RootPageNode

interface PageTransformer {
    operator fun invoke(input: RootPageNode): RootPageNode
}

object SourceSetMergePageTransformer : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {

        return input.transformContentPagesTree { contentPage ->
            val content: ContentNode = contentPage.content
            TODO()
        }
    }

}
