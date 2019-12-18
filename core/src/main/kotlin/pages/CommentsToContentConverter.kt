package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.doc.DocTag

interface CommentsToContentConverter {
    fun buildContent(
        docTag: DocTag,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style> = emptySet(),
        extras: Set<Extra> = emptySet()
    ): List<ContentNode>
}
