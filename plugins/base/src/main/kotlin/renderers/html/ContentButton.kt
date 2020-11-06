package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.pages.Style

data class ContentButton(
    val label: String,
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentButton = copy(extra = newExtras)
    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): ContentButton = copy(sourceSets = sourceSets)
    override fun hasAnyContent(): Boolean = !label.isBlank()
}
