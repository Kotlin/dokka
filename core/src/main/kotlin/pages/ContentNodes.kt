package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI

data class DCI(val dri: DRI, val kind: Kind) {
    override fun toString() = "$dri[$kind]"
}

interface ContentNode {
    val dci: DCI
    val platforms: Set<PlatformData>
    val style: Set<Style>
    val extras: Set<Extra>
}

/** Simple text */
data class ContentText(
    val text: String,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extras: Set<Extra> = emptySet()
) : ContentNode

/** Headers */
data class ContentHeader(
    override val children: List<ContentNode>,
    val level: Int,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extras: Set<Extra> = emptySet()
) : ContentComposite {
    constructor(level: Int, c: ContentComposite) : this(c.children, level, c.dci, c.platforms, c.style, c.extras)
}

/** Code blocks */
data class ContentCode(
    override val children: List<ContentNode>,
    val language: String,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extras: Set<Extra>
) : ContentComposite

/** Union type replacement */
interface ContentLink : ContentComposite

/** All links to classes, packages, etc. that have te be resolved */
data class ContentDRILink(
    override val children: List<ContentNode>,
    val address: DRI,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extras: Set<Extra> = emptySet()
) : ContentLink

/** All links that do not need to be resolved */
data class ContentResolvedLink(
    override val children: List<ContentNode>,
    val address: String,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extras: Set<Extra> = emptySet()
) : ContentLink

/** All links that do not need to be resolved */
data class ContentEmbeddedResource(
    val address: String,
    val altText: String?,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style> = emptySet(),
    override val extras: Set<Extra> = emptySet()
) : ContentLink {
    override val children = emptyList<ContentNode>()
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
    override val extras: Set<Extra>
) : ContentComposite

/** Lists */
data class ContentList(
    override val children: List<ContentNode>,
    val ordered: Boolean,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extras: Set<Extra>
) : ContentComposite

/** Default group, eg. for blocks of Functions, Properties, etc. **/
data class ContentGroup(
    override val children: List<ContentNode>,
    override val dci: DCI,
    override val platforms: Set<PlatformData>,
    override val style: Set<Style>,
    override val extras: Set<Extra>
) : ContentComposite

/** All extras */
interface Extra
interface Style
interface Kind

enum class ContentKind : Kind {
    Comment, Functions, Parameters, Properties, Classes, Packages, Symbol, Sample, Main
}

enum class TextStyle : Style {
    Bold, Italic, Strong, Strikethrough, Paragraph
}

interface HTMLMetadata: Extra {
    val key: String
    val value: String
}

data class HTMLSimpleAttr(override val key: String, override val value: String): HTMLMetadata
data class HTMLTableMetadata(val item: String, override val key: String, override val value: String): HTMLMetadata

fun ContentNode.dfs(predicate: (ContentNode) -> Boolean): ContentNode? = if (predicate(this)) {
    this
} else {
    if (this is ContentComposite) {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    } else {
        null
    }
}
