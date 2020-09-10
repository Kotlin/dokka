package org.jetbrains.dokka.base.transformers.pages.serialization

import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

interface PagesSerializationContentView : ContentNode

//TODO remove redundancy, this can be done in interface
data class TextView(
    override val text: String,
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>
) : Text {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): Text = copy(extra = newExtras)
    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): Text = copy(sourceSets = sourceSets)
    override fun hasAnyContent(): Boolean = !text.isBlank()
}

data class BreakLineView(
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>
) : BreakLine {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): BreakLine = copy(extra = newExtras)
    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): BreakLine = copy(sourceSets = sourceSets)
    override fun hasAnyContent(): Boolean = true
}

data class HeaderView(
    override val level: Int,
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>,
    override val children: List<ContentNode>
) : Header {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): Header = copy(extra = newExtras)

    override fun transformChildren(transformer: (ContentNode) -> ContentNode): HeaderView =
        copy(children = children.map(transformer))

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): HeaderView =
        copy(sourceSets = sourceSets)
}




