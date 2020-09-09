package org.jetbrains.dokka.processing.translators.docTags

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

interface CommentsToContentTranslator {
    fun buildContent(
        docTag: DocTag,
        dci: DCI,
        sourceSets: Set<DokkaSourceSet>,
        styles: Set<Style> = emptySet(),
        extras: PropertyContainer<ContentNode> = PropertyContainer.empty()
    ): List<ContentNode>
}
