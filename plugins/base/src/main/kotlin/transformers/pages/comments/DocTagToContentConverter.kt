package org.jetbrains.dokka.base.transformers.pages.comments

import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.plus
import org.jetbrains.dokka.model.toDisplaySourceSets
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

open class DocTagToContentConverter : CommentsToContentConverter {
    override fun buildContent(
        docTag: DocTag,
        dci: DCI,
        sourceSets: Set<DokkaSourceSet>,
        styles: Set<Style>,
        extras: PropertyContainer<ContentNode>
    ): List<ContentNode> {

        fun buildChildren(docTag: DocTag, newStyles: Set<Style> = emptySet(), newExtras: SimpleAttr? = null) =
            docTag.children.flatMap {
                buildContent(it, dci, sourceSets, styles + newStyles, newExtras?.let { extras + it } ?: extras)
            }

        fun buildTableRows(rows: List<DocTag>, newStyle: Style): List<ContentGroup> =
            rows.flatMap {
                @Suppress("UNCHECKED_CAST")
                buildContent(it, dci, sourceSets, styles + newStyle, extras) as List<ContentGroup>
            }

        fun buildHeader(level: Int) =
            listOf(
                ContentHeader(
                    buildChildren(docTag),
                    level,
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )

        fun buildList(ordered: Boolean, newStyles: Set<Style> = emptySet(), start: Int = 1) =
            listOf(
                ContentList(
                    buildChildren(docTag),
                    ordered,
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles + newStyles,
                    ((PropertyContainer.empty<ContentNode>()) + SimpleAttr("start", start.toString()))
                )
            )

        fun buildNewLine() = listOf(
            ContentBreakLine(
                sourceSets.toDisplaySourceSets()
            )
        )

        fun P.collapseParagraphs(): P =
            if (children.size == 1 && children.first() is P) (children.first() as P).collapseParagraphs() else this

        return when (docTag) {
            is H1 -> buildHeader(1)
            is H2 -> buildHeader(2)
            is H3 -> buildHeader(3)
            is H4 -> buildHeader(4)
            is H5 -> buildHeader(5)
            is H6 -> buildHeader(6)
            is Ul -> buildList(false)
            is Ol -> buildList(true, start = docTag.params["start"]?.toInt() ?: 1)
            is Li -> listOf(
                ContentGroup(buildChildren(docTag), dci, sourceSets.toDisplaySourceSets(), styles, extras)
            )
            is Dl -> buildList(false, newStyles = setOf(ListStyle.DescriptionList))
            is Dt -> listOf(
                ContentGroup(
                    buildChildren(docTag),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles + ListStyle.DescriptionTerm
                )
            )
            is Dd -> listOf(
                ContentGroup(
                    buildChildren(docTag),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles + ListStyle.DescriptionDetails
                )
            )
            is Br -> buildNewLine()
            is B -> buildChildren(docTag, setOf(TextStyle.Strong))
            is I -> buildChildren(docTag, setOf(TextStyle.Italic))
            is P -> listOf(
                ContentGroup(
                    buildChildren(docTag.collapseParagraphs()),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles + setOf(TextStyle.Paragraph),
                    extras
                )
            )
            is A -> listOf(
                ContentResolvedLink(
                    buildChildren(docTag),
                    docTag.params.getValue("href"),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )
            is DocumentationLink -> listOf(
                ContentDRILink(
                    buildChildren(docTag),
                    docTag.dri,
                    DCI(
                        setOf(docTag.dri),
                        ContentKind.Main
                    ),
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )
            is BlockQuote, is Pre, is CodeBlock -> listOf(
                ContentCodeBlock(
                    buildChildren(docTag),
                    docTag.params.getOrDefault("lang", ""),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )
            is CodeInline -> listOf(
                ContentCodeInline(
                    buildChildren(docTag),
                    "",
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )
            is Img -> listOf(
                ContentEmbeddedResource(
                    address = docTag.params["href"]!!,
                    altText = docTag.params["alt"],
                    dci = dci,
                    sourceSets = sourceSets.toDisplaySourceSets(),
                    style = styles,
                    extra = extras
                )
            )
            is HorizontalRule -> listOf(
                ContentText(
                    "",
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    setOf()
                )
            )
            is Text -> listOf(
                ContentText(
                    docTag.body,
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles,
                    extras + HtmlContent.takeIf { docTag.params["content-type"] == "html" }
                )
            )
            is Strikethrough -> buildChildren(docTag, setOf(TextStyle.Strikethrough))
            is Table -> {
                //https://html.spec.whatwg.org/multipage/tables.html#the-caption-element
                if (docTag.children.any { it is TBody }) {
                    val head = docTag.children.filterIsInstance<THead>().flatMap { it.children }
                    val body = docTag.children.filterIsInstance<TBody>().flatMap { it.children }
                    listOf(
                        ContentTable(
                            header = buildTableRows(head.filterIsInstance<Th>(), CommentTable),
                            caption = docTag.children.firstIsInstanceOrNull<Caption>()?.let {
                                ContentGroup(
                                    buildContent(it, dci, sourceSets),
                                    dci,
                                    sourceSets.toDisplaySourceSets(),
                                    styles,
                                    extras
                                )
                            },
                            buildTableRows(body.filterIsInstance<Tr>(), CommentTable),
                            dci,
                            sourceSets.toDisplaySourceSets(),
                            styles + CommentTable
                        )
                    )
                } else {
                    listOf(
                        ContentTable(
                            header = buildTableRows(docTag.children.filterIsInstance<Th>(), CommentTable),
                            caption = null,
                            buildTableRows(docTag.children.filterIsInstance<Tr>(), CommentTable),
                            dci,
                            sourceSets.toDisplaySourceSets(),
                            styles + CommentTable
                        )
                    )
                }
            }
            is Th,
            is Tr -> listOf(
                ContentGroup(
                    docTag.children.map {
                        ContentGroup(buildChildren(it), dci, sourceSets.toDisplaySourceSets(), styles, extras)
                    },
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )
            is Index -> listOf(
                ContentGroup(
                    buildChildren(docTag, newStyles = styles + ContentStyle.InDocumentationAnchor),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles
                )
            )
            is CustomDocTag -> if (docTag.isNonemptyFile()) {
                listOf(
                    ContentGroup(
                        buildChildren(docTag),
                        dci,
                        sourceSets.toDisplaySourceSets(),
                        styles,
                        extra = extras
                    )
                )
            } else {
                buildChildren(docTag)
            }
            is Caption -> listOf(
                ContentGroup(
                    buildChildren(docTag),
                    dci,
                    sourceSets.toDisplaySourceSets(),
                    styles + ContentStyle.Caption,
                    extra = extras
                )
            )

            else -> buildChildren(docTag)
        }
    }

    private fun CustomDocTag.isNonemptyFile() = name == MarkdownElementTypes.MARKDOWN_FILE.name && children.size > 1
}
