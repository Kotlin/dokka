package org.jetbrains.dokka.base.transformers.pages.comments

import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext

object DocTagToContentConverter : CommentsToContentConverter {
    override fun buildContent(
        docTag: DocTag,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style>,
        extra: PropertyContainer<ContentNode>
    ): List<ContentNode> {

        fun buildChildren(docTag: DocTag, newStyles: Set<Style> = emptySet(), newExtras: SimpleAttr? = null) =
            docTag.children.flatMap {
                buildContent(it, dci, platforms, styles + newStyles, newExtras?.let { extra + it } ?: extra)
            }

        fun buildHeader(level: Int) =
            listOf(
                ContentHeader(
                    buildChildren(docTag),
                    level,
                    dci,
                    platforms,
                    styles
                )
            )

        fun buildList(ordered: Boolean) =
            listOf(
                ContentList(
                    buildChildren(docTag),
                    ordered,
                    dci,
                    platforms,
                    styles
                )
            )

        return when (docTag) {
            is H1 -> buildHeader(1)
            is H2 -> buildHeader(2)
            is H3 -> buildHeader(3)
            is H4 -> buildHeader(4)
            is H5 -> buildHeader(5)
            is H6 -> buildHeader(6)
            is Ul -> buildList(false)
            is Ol -> buildList(true)
            is Li -> buildChildren(docTag)
            is B -> buildChildren(docTag, setOf(TextStyle.Strong))
            is I -> buildChildren(docTag, setOf(TextStyle.Italic))
            is P -> buildChildren(docTag, newStyles = setOf(TextStyle.Paragraph))
            is A -> listOf(
                ContentResolvedLink(
                    buildChildren(docTag),
                    docTag.params.get("href")!!,
                    dci,
                    platforms,
                    styles
                )
            )
            is DocumentationLink -> listOf(
                ContentDRILink(
                    buildChildren(docTag),
                    docTag.dri,
                    DCI(
                        setOf(docTag.dri),
                        ContentKind.Symbol
                    ),
                    platforms,
                    styles
                )
            )
            is BlockQuote -> listOf(
                ContentCode(
                    buildChildren(docTag),
                    "",
                    dci,
                    platforms,
                    styles
                )
            )
            is Code -> listOf(
                ContentCode(
                    buildChildren(docTag),
                    "",
                    dci,
                    platforms,
                    styles
                )
            )
            is Img -> listOf(
                ContentEmbeddedResource(
                    address = docTag.params["href"]!!,
                    altText = docTag.params["alt"],
                    dci = dci,
                    platforms = platforms,
                    style = styles,
                    extra = extra
                )
            )
            is HorizontalRule -> listOf(
                ContentText(
                    "",
                    dci,
                    platforms,
                    setOf()
                )
            )
            is Text -> listOf(
                ContentText(
                    docTag.body,
                    dci,
                    platforms,
                    styles
                )
            )
            else -> buildChildren(docTag)
        }
    }
}
