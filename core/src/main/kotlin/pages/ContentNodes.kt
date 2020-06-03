package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties

data class DCI(val dri: Set<DRI>, val kind: Kind) {
    override fun toString() = "$dri[$kind]"
}

interface ContentNode : WithExtraProperties<ContentNode> {
    val dci: DCI
    val sourceSets: Set<SourceSetData>
    val style: Set<Style>
}

/** Simple text */
data class ContentText(
    val text: String,
    override val dci: DCI,
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

// TODO: Remove
data class ContentBreakLine(
    override val sourceSets: Set<SourceSetData>,
    override val dci: DCI = DCI(emptySet(), ContentKind.Empty),
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

/** Headers */
data class ContentHeader(
    override val children: List<ContentNode>,
    val level: Int,
    override val dci: DCI,
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    constructor(level: Int, c: ContentComposite) : this(c.children, level, c.dci, c.sourceSets, c.style, c.extra)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentHeader = copy(extra = newExtras)
}

/** Code blocks */
data class ContentCode(
    override val children: List<ContentNode>,
    val language: String,
    override val dci: DCI,
    override val sourceSets: Set<SourceSetData>,
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
    override val sourceSets: Set<SourceSetData>,
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
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentLink {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentResolvedLink =
        copy(extra = newExtras)
}

/** Embedded resources like images */
data class ContentEmbeddedResource(
    override val children: List<ContentNode> = emptyList(),
    val address: String,
    val altText: String?,
    override val dci: DCI,
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentLink {
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
    override val sourceSets: Set<SourceSetData>,
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
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentList = copy(extra = newExtras)
}

/** Default group, eg. for blocks of Functions, Properties, etc. **/
data class ContentGroup(
    override val children: List<ContentNode>,
    override val dci: DCI,
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentGroup = copy(extra = newExtras)
}

/**
 * @property groupName is used for finding and copying [ContentDivergentInstance]s when merging [ContentPage]s
 */
data class ContentDivergentGroup(
    override val children: List<ContentDivergentInstance>,
    override val dci: DCI,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>,
    val groupID: GroupID,
    val implicitlySourceSetHinted: Boolean = true
) : ContentComposite {
    data class GroupID(val name: String)

    override val sourceSets: Set<SourceSetData>
        get() = children.flatMap { it.sourceSets }.distinct().toSet()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentDivergentGroup =
        copy(extra = newExtras)
}

/** Instance of a divergent content */
data class ContentDivergentInstance(
    val before: ContentNode?,
    val divergent: ContentNode,
    val after: ContentNode?,
    override val dci: DCI,
    override val sourceSets: Set<SourceSetData>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentComposite {
    override val children: List<ContentNode>
        get() = listOfNotNull(before, divergent, after)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentDivergentInstance =
        copy(extra = newExtras)
}

data class PlatformHintedContent(
    val inner: ContentNode,
    override val sourceSets: Set<SourceSetData>
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

interface Style
interface Kind

enum class ContentKind : Kind {

    Comment, Constructors, Functions, Parameters, Properties, Classlikes, Packages, Symbol, Sample, Main, BriefComment,
    Empty, Source, TypeAliases, Cover, Inheritors, SourceSetDependantHint;

    companion object {
        private val platformTagged =
            setOf(Constructors, Functions, Properties, Classlikes, Packages, Source, TypeAliases, Inheritors)

        fun shouldBePlatformTagged(kind: Kind): Boolean = kind in platformTagged
    }
}

enum class TextStyle : Style {
    Bold, Italic, Strong, Strikethrough, Paragraph, Block, Span, Monospace, Indented, BreakableAfter, Breakable
}

enum class ContentStyle : Style {
    RowTitle, TabbedContent, TabbedContentBody, WithExtraAttributes
}

object CommentTable: Style

fun ContentNode.dfs(predicate: (ContentNode) -> Boolean): ContentNode? = if (predicate(this)) {
    this
} else {
    if (this is ContentComposite) {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    } else {
        null
    }
}

fun ContentNode.hasStyle(style: Style) = this.style.contains(style)
