package org.jetbrains.dokka.pages

import org.jetbrains.dokka.markdown.MarkdownNode
import org.jetbrains.dokka.links.DRI

interface MarkdownToContentConverter {
    fun buildContent(
        node: MarkdownNode,
        dci: DCI,
        platforms: Set<PlatformData>,
        links: Map<String, DRI> = emptyMap(),
        styles: Set<Style> = emptySet(),
        extras: Set<Extra> = emptySet()
    ): List<ContentNode>
}
