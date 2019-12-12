package org.jetbrains.dokka.pages

import model.doc.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.dokka.markdown.MarkdownNode
import org.jetbrains.dokka.plugability.DokkaContext

class DocNodeToContentConverter(
    private val context: DokkaContext
) : MarkdownToContentConverter {
    override fun buildContent(
        docNode: DocNode,
        dci: DCI,
        platforms: Set<PlatformData>,
        styles: Set<Style>,
        extras: Set<Extra>

    ): List<ContentNode> {

        fun buildChildren(docNode: DocNode, newStyles: Set<Style> = emptySet(), newExtras: Set<Extra> = emptySet()) =
            docNode.children.flatMap {
                buildContent(it, dci, platforms, styles + newStyles, extras + newExtras)
            }

        fun buildHeader(level: Int) =
            listOf(ContentHeader(buildChildren(docNode), level, dci, platforms, styles, extras))

        fun buildList(ordered: Boolean) =
            listOf(ContentList(buildChildren(docNode), ordered, dci, platforms, styles, extras))

        return when (docNode) {
            is H1 -> buildHeader(1)
            is H2 -> buildHeader(2)
            is H3 -> buildHeader(3)
            is H4 -> buildHeader(4)
            is H5 -> buildHeader(5)
            is H6 -> buildHeader(6)
            is Ul -> buildList(false)
            is Ol -> buildList(true)
            is Li -> buildChildren(docNode)
            is B -> buildChildren(docNode, setOf(TextStyle.Strong))
            is I -> buildChildren(docNode, setOf(TextStyle.Italic))
            is P -> buildChildren(docNode, newStyles = setOf(TextStyle.Paragraph))
            is A -> listOf(
                ContentResolvedLink(
                    buildChildren(docNode),
                    docNode.params.get("href")!!,
                    dci,
                    platforms,
                    styles,
                    extras
                )
            )
            is DocumentationLink -> listOf(
                ContentDRILink(
                    buildChildren(docNode),
                    docNode.dri,
                    DCI(docNode.dri, ContentKind.Symbol),
                    platforms,
                    styles,
                    extras
                )
            )
            is BlockQuote -> throw NotImplementedError("Implement DocNotToContent BlockQuote!")
            is Code -> listOf(
                ContentCode(
                    buildChildren(docNode),
                    "",
                    dci,
                    platforms,
                    styles,
                    extras
                )
            )
            is Img -> throw NotImplementedError("Implement DocNotToContent Img!")
            is HorizontalRule -> throw NotImplementedError("Implement DocNotToContent HorizontalRule!")
            is Text -> listOf(ContentText(docNode.body, dci, platforms, styles, extras))
            else -> buildChildren(docNode)
        }
    }
}
