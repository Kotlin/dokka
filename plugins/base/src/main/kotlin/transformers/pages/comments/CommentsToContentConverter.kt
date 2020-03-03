package org.jetbrains.dokka.base.transformers.pages.comments

import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

interface CommentsToContentConverter {
    fun buildContent(
        docTag: DocTag,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style> = emptySet(),
        extras: PropertyContainer<ContentNode> = PropertyContainer.empty()
    ): List<ContentNode>
}
