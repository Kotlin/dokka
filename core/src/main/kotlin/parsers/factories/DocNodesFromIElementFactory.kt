package org.jetbrains.dokka.parsers.factories

import model.doc.*
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.dokka.links.DRI
import java.lang.NullPointerException

object DocNodesFromIElementFactory {
    fun getInstance(type: IElementType, children: List<DocNode> = emptyList(), params: Map<String, String> = emptyMap(), body: String? = null, dri: DRI? = null) =
        when(type) {
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK    -> if(dri == null) A(children, params) else DocumentationLink(children, params, dri)
            MarkdownElementTypes.STRONG                 -> B(children, params)
            MarkdownElementTypes.BLOCK_QUOTE            -> BlockQuote(children, params)
            MarkdownElementTypes.CODE_SPAN,
            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.CODE_FENCE             -> Code(children, params)
            MarkdownElementTypes.ATX_1                  -> H1(children, params)
            MarkdownElementTypes.ATX_2                  -> H2(children, params)
            MarkdownElementTypes.ATX_3                  -> H3(children, params)
            MarkdownElementTypes.ATX_4                  -> H4(children, params)
            MarkdownElementTypes.ATX_5                  -> H5(children, params)
            MarkdownElementTypes.ATX_6                  -> H6(children, params)
            MarkdownElementTypes.EMPH                   -> I(children, params)
            MarkdownElementTypes.IMAGE                  -> Img(children, params)
            MarkdownElementTypes.LIST_ITEM              -> Li(children, params)
            MarkdownElementTypes.ORDERED_LIST           -> Ol(children, params)
            MarkdownElementTypes.UNORDERED_LIST         -> Ul(children, params)
            MarkdownElementTypes.PARAGRAPH              -> P(children, params)
            MarkdownTokenTypes.TEXT                     -> Text(children, params, body ?: throw NullPointerException("Text body should be at least empty string passed to DocNodes factory!"))
            MarkdownTokenTypes.HORIZONTAL_RULE          -> HorizontalRule()
            else                                        -> CustomNode(children, params)
        }
}