package org.jetbrains.dokka.pages

import model.doc.DocNode
import org.jetbrains.dokka.markdown.MarkdownNode
import org.jetbrains.dokka.links.DRI

interface MarkdownToContentConverter {
    fun buildContent(
        docNode: DocNode,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style> = emptySet(),
        extras: Set<Extra> = emptySet()
    ): List<ContentNode>
}
