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
            }.coalesceText(platforms, styles + newStyles, extras + newExtras)

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
            is Text -> listOf(ContentText(docNode.body, dci, platforms, styles, extras))
            else -> buildChildren(docNode)


//            MarkdownElementTypes.LIST_ITEM -> TODO()
//            MarkdownElementTypes.CODE_SPAN -> TODO()

//            val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
//            if (startDelimiter != null) {
//                val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
//                val codeSpan = ContentCode().apply { append(ContentText(text)) }
//                parent.append(codeSpan)
//            }

//            MarkdownElementTypes.CODE_BLOCK,
//            MarkdownElementTypes.CODE_FENCE -> {
//                val language = node.child(MarkdownTokenTypes.FENCE_LANG)?.text?.trim() ?: ""
//                listOf(ContentCode(buildChildren(node), language, dci, platforms, styles, extras)) // TODO
//            }

//            MarkdownElementTypes.INLINE_LINK -> {
//            val linkTextNode = node.child(MarkdownElementTypes.LINK_TEXT)
//            val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)
//            if (linkTextNode != null) {
//                if (destination != null) {
//                    val link = ContentExternalLink(destination.text)
//                    renderLinkTextTo(linkTextNode, link, linkResolver)
//                    parent.append(link)
//                } else {
//                    val link = ContentExternalLink(linkTextNode.getLabelText())
//                    renderLinkTextTo(linkTextNode, link, linkResolver)
//                    parent.append(link)
//                }
//            }
//                TODO: Linking!!!
//            ContentLink()
//                TODO()
//
//            }


//            MarkdownTokenTypes.CODE_LINE -> {
//                listOf(ContentText(node.text, dci, platforms, styles, extras)) // TODO check
//            if (parent is ContentBlockCode) {
//                parent.append(content)
//            } else {
//                parent.append(ContentBlockCode().apply { append(content) })
//            }
//            }

//            MarkdownTokenTypes.TEXT ->
//            fun createEntityOrText(text: String): ContentNode {
//                if (text == "&amp;" || text == "&quot;" || text == "&lt;" || text == "&gt;") {
//                    return ContentEntity(text)
//                }
//                if (text == "&") {
//                    return ContentEntity("&amp;")
//                }
//                val decodedText = EntityConverter.replaceEntities(text, true, true)
//                if (decodedText != text) {
//                    return ContentEntity(text)
//                }
//                return ContentText(text)
//            }
//
//            parent.append(createEntityOrText(node.text))
//                listOf(ContentText(node.text, dci, platforms, styles, extras)) // TODO


//            MarkdownElementTypes.LINK_DEFINITION -> TODO()
//            MarkdownTokenTypes.EMAIL_AUTOLINK ->
//                listOf(
//                    ContentResolvedLink(
//                        listOf(ContentText(node.text, dci, platforms, styles, extras)),
//                        "mailto:${node.text}",
//                        dci, platforms, styles, extras
//                    )
//                )
        }
    }

    private fun Collection<ContentNode>.coalesceText(platforms: Set<PlatformData>, styles: Set<Style>, extras: Set<Extra>) =
        this
            .sliceWhen { prev, next -> prev::class != next::class }
            .flatMap { nodes ->
                when (nodes.first()) {
                    is ContentText -> listOf(
                        ContentText(
                            nodes.joinToString("") { (it as ContentText).text },
                            nodes.first().dci,
                            platforms,
                            styles,
                            extras
                        )
                    )
                    else -> nodes
                }
            }
}

fun <T> Collection<T>.sliceWhen(predicate: (before: T, after: T) -> Boolean): Collection<Collection<T>> {
    val newCollection = mutableListOf<Collection<T>>()
    var currentSlice = mutableListOf<T>()
    for ((prev, next) in this.windowed(2, 1, false)) {
        currentSlice.add(prev)
        if (predicate(prev, next)) {
            newCollection.add(currentSlice)
            currentSlice = mutableListOf<T>()
        }
    }
    if (this.isNotEmpty()) {
        currentSlice.add(this.last())
        newCollection.add(currentSlice)
    }
    return newCollection
}