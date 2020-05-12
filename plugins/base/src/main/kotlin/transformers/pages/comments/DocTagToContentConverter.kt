package org.jetbrains.dokka.base.transformers.pages.comments

import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

object DocTagToContentConverter : CommentsToContentConverter {
    override fun buildContent(
        docTag: DocTag,
        dci: DCI,
        sourceSets: Set<SourceSetData>,
        styles: Set<Style>,
        extra: PropertyContainer<ContentNode>
    ): List<ContentNode> {

        fun buildChildren(docTag: DocTag, newStyles: Set<Style> = emptySet(), newExtras: SimpleAttr? = null) =
            docTag.children.flatMap {
                buildContent(it, dci, sourceSets, styles + newStyles, newExtras?.let { extra + it } ?: extra)
            }

        fun buildTableRows(rows: List<DocTag>, newStyle: Style): List<ContentGroup> =
            rows.flatMap {
                buildContent(it, dci, sourceSets, styles + newStyle, extra) as List<ContentGroup>
            }

        fun buildHeader(level: Int) =
            listOf(
                ContentHeader(
                    buildChildren(docTag),
                    level,
                    dci,
                    sourceSets,
                    styles
                )
            )

        fun buildList(ordered: Boolean, start: Int = 1) =
            listOf(
                ContentList(
                    buildChildren(docTag),
                    ordered,
                    dci,
                    sourceSets,
                    styles,
                    ((PropertyContainer.empty<ContentNode>()) + SimpleAttr("start", start.toString()))
                )
            )

        fun buildNewLine() = listOf(
            ContentBreakLine(
                sourceSets
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
            is Ol -> buildList(true, docTag.params["start"]?.toInt() ?: 1)
            is Li -> buildChildren(docTag)
            is Br -> buildNewLine()
            is B -> buildChildren(docTag, setOf(TextStyle.Strong))
            is I -> buildChildren(docTag, setOf(TextStyle.Italic))
            is P -> buildChildren(docTag, newStyles = setOf(TextStyle.Paragraph))
            is A -> listOf(
                ContentResolvedLink(
                    buildChildren(docTag),
                    docTag.params.get("href")!!,
                    dci,
                    sourceSets,
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
                    sourceSets,
                    styles
                )
            )
            is BlockQuote -> listOf(
                ContentCode(
                    buildChildren(docTag),
                    "",
                    dci,
                    sourceSets,
                    styles
                )
            )
            is Code -> listOf(
                ContentCode(
                    buildChildren(docTag),
                    "",
                    dci,
                    sourceSets,
                    styles
                )
            )
            is Img -> listOf(
                ContentEmbeddedResource(
                    address = docTag.params["href"]!!,
                    altText = docTag.params["alt"],
                    dci = dci,
                    sourceSets = sourceSets,
                    style = styles,
                    extra = extra
                )
            )
            is HorizontalRule -> listOf(
                ContentText(
                    "",
                    dci,
                    sourceSets,
                    setOf()
                )
            )
            is Text -> listOf(
                ContentText(
                    docTag.body,
                    dci,
                    sourceSets,
                    styles
                )
            )
            is Strikethrough -> buildChildren(docTag, setOf(TextStyle.Strikethrough))
            is Table -> listOf(
                ContentTable(
                    buildTableRows(docTag.children.filterIsInstance<Th>(), CommentTable),
                    buildTableRows(docTag.children.filterIsInstance<Tr>(), CommentTable),
                    dci,
                    sourceSets,
                    styles + CommentTable
                )
            )
            is Th,
            is Tr -> listOf(
                ContentGroup(
                    docTag.children.map {
                        ContentGroup(buildChildren(it), dci, sourceSets, styles, extra)
                    },
                    dci,
                    sourceSets,
                    styles
                )
            )
            else -> buildChildren(docTag)
        }
    }
}
