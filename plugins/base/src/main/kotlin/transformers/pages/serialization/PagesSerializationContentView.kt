package org.jetbrains.dokka.base.transformers.pages.serialization

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

interface PagesSerializationContentView : Content

data class TextView(
    override val text: String,
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<Content>
) : Text, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class BreakLineView(
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<Content>
) : BreakLine, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class HeaderView(
    override val level: Int,
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<Content>,
    override val children: List<Content>
) : Header, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class CodeView(
    override val language: String,
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<Content>,
) : Code, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class ResolvedLinkView(
    override val address: String,
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<Content>,
) : ResolvedLink, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class UnresolvedLinkView(
    override val address: DRI,
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<Content>,
) : UnresolvedLink, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class TableView(
    override val header: List<Content>,
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<Content>
) : Table, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class ListView(
    override val ordered: Boolean,
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<Content>
) : ElementList, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class GroupView(
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<Content>
) : Group, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

//TODO shouldnt divergent group be a group?
data class DivergentGroupView(
    override val groupID: ContentDivergentGroup.GroupID,
    override val implicitlySourceSetHinted: Boolean,
    override val dci: DCI,
    override val style: Set<Style>,
    override val children: List<Content>,
    override val extra: PropertyContainer<Content>,
    override val sourceSets: Set<DisplaySourceSet>
) : DivergentGroup, PagesSerializationContentView {
    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class DivergentInstanceView(
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val before: Content?,
    override val divergent: Content,
    override val after: Content?,
    override val extra: PropertyContainer<Content>
) : DivergentInstance, PagesSerializationContentView {
    override val children: List<Content>
        get() = listOfNotNull(before, divergent, after)

    override fun withNewExtras(newExtras: PropertyContainer<Content>): Content = copy(extra = newExtras)
}

data class PlatformHintedContentView(
    override val sourceSets: Set<DisplaySourceSet>,
    override val inner: Content,
) : PlatformHinted, PagesSerializationContentView {
    override val children = listOf(inner)
    override val dci: DCI
        get() = inner.dci

    override val extra: PropertyContainer<Content>
        get() = inner.extra

    override val style: Set<Style>
        get() = inner.style

    override fun withNewExtras(newExtras: PropertyContainer<Content>) =
        throw UnsupportedOperationException("This method should not be called on this PlatformHintedContent")
}




