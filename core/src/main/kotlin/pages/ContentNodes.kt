package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties

data class DCI(val dri: Set<DRI>, val kind: Kind) {
    override fun toString() = "$dri[$kind]"
}

interface ContentNode : WithExtraProperties<ContentNode> {
    val dci: DCI
    val platforms: Set<PlatformData>
    val style: Set<Style>
}

/** Simple text */
data class ContentText(
    val text: String,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

data class ContentBreakLine(
    override val platforms: Set<PlatformData>,
    override val dci: DCI = DCI(emptySet(), ContentKind.Empty),
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
): ContentNode {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

/** Headers */
data class ContentHeader(
    override val children: List<ContentNode>,
    val level: Int,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    constructor(level: Int, c: ContentComposite) : this(c.children, level, c.dci, c.platforms, c.style, c.extra)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentHeader = copy(extra = newExtras)
}

/** Code blocks */
data class ContentCode(
    override val children: List<ContentNode>,
    val language: String,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentCode = copy(extra = newExtras)
}

/** Union type replacement */
interface ContentLink : ContentComposite

/** All links to classes, packages, etc. that have te be resolved */
data class ContentDRILink(
    override val children: List<ContentNode>,
    val address: DRI,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentLink {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentDRILink = copy(extra = newExtras)
}

/** All links that do not need to be resolved */
data class ContentResolvedLink(
    override val children: List<ContentNode>,
    val address: String,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentLink {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentResolvedLink =
        copy(extra = newExtras)
}

/** All links that do not need to be resolved */
data class ContentEmbeddedResource(
    val address: String,
    val altText: String?,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentLink {
    override val children = emptyList<ContentNode>()
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentEmbeddedResource =
        copy(extra = newExtras)
}

/** Logical grouping of [ContentNode]s  */
interface ContentComposite : ContentNode {
    val children: List<ContentNode>
}

/** Tables */
data class ContentTable(
    val header: List<ContentGroup>,
    override val children: List<ContentGroup>,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentTable = copy(extra = newExtras)
}

/** Lists */
data class ContentList(
    override val children: List<ContentNode>,
    val ordered: Boolean,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentList = copy(extra = newExtras)
}

/** Default group, eg. for blocks of Functions, Properties, etc. **/
data class ContentGroup(
    override val children: List<ContentNode>,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentGroup = copy(extra = newExtras)
}

data class PlatformHintedContent(
    val inner: ContentNode,
    override val platforms: Set<PlatformData>
) : ContentComposite {
    override val children = listOf(inner)

    override val dci: DCI
        get() = inner.dci

    override val extra: PropertyContainer<ContentNode>
        get() = inner.extra

    override val style: Set<Style>
        get() = inner.style

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>) =
        throw UnsupportedOperationException("This method should not be called on this PlatformHintedContent")
}

/** All extras */
interface Extra
interface Style
interface Kind

enum class ContentKind : Kind {
    Comment, Constructors, Functions, Parameters, Properties, Classlikes, Packages, Symbol, Sample, Main, BriefComment, Empty
}

enum class TextStyle : Style {
    Bold, Italic, Strong, Strikethrough, Paragraph, Block, Monospace, Indented
}

fun ContentNode.dfs(predicate: (ContentNode) -> Boolean): ContentNode? = if (predicate(this)) {
    this
} else {
    if (this is ContentComposite) {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    } else {
        null
    }
}
