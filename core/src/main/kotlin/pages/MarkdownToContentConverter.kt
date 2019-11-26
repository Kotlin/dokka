package org.jetbrains.dokka.pages

import org.jetbrains.dokka.MarkdownNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

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
