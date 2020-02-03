package org.jetbrains.dokka.parsers

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.model.doc.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.impl.ListItemCompositeNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.parsers.factories.DocNodesFromIElementFactory
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

class MarkdownParser (
    private val resolutionFacade: DokkaResolutionFacade,
    private val declarationDescriptor: DeclarationDescriptor
    ) : Parser() {

    inner class MarkdownVisitor(val text: String) {

        private fun headersHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                visitNode(node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }!!).children
            )

        private fun horizontalRulesHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.HORIZONTAL_RULE)

        private fun emphasisHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                children = listOf(visitNode(node.children[node.children.size / 2]))
            )

        private fun blockquotesHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(node.type, children = node.children.drop(1).map { visitNode(it) })

        private fun listsHandler(node: ASTNode): DocTag {

            val children = node.children.filterIsInstance<ListItemCompositeNode>().flatMap {
                if( it.children.last().type in listOf(MarkdownElementTypes.ORDERED_LIST, MarkdownElementTypes.UNORDERED_LIST) ) {
                    val nestedList = it.children.last()
                    (it.children as MutableList).removeAt(it.children.lastIndex)
                    listOf(it, nestedList)
                }
                else
                    listOf(it)
            }

            return DocNodesFromIElementFactory.getInstance(
                node.type,
                children =
                children
                    .map {
                        if(it.type == MarkdownElementTypes.LIST_ITEM)
                            DocNodesFromIElementFactory.getInstance(
                                it.type,
                                children = it
                                    .children
                                    .drop(1)
                                    .evaluateChildren()
                            )
                        else
                            visitNode(it)
                    },
                params =
                if (node.type == MarkdownElementTypes.ORDERED_LIST) {
                    val listNumberNode = node.children.first().children.first()
                    mapOf("start" to text.substring(listNumberNode.startOffset, listNumberNode.endOffset).dropLast(2))
                } else
                    emptyMap()
            )
        }

        private fun linksHandler(node: ASTNode): DocTag {
            val linkNode = node.children.find { it.type == MarkdownElementTypes.LINK_LABEL }!!
            val link = text.substring(linkNode.startOffset + 1, linkNode.endOffset - 1)

            val dri: DRI? = if (link.startsWith("http") || link.startsWith("www")) {
                null
            } else {
                    resolveKDocLink(
                        resolutionFacade.resolveSession.bindingContext,
                        resolutionFacade,
                        declarationDescriptor,
                        null,
                        link.split('.')
                    ).also { if (it.size > 1) DokkaConsoleLogger.warn("Markdown link resolved more than one element: $it") }.firstOrNull()//.single()
                        ?.let { DRI.from(it) }

            }
            val href = mapOf("href" to link)
            return when (node.type) {
                MarkdownElementTypes.FULL_REFERENCE_LINK -> DocNodesFromIElementFactory.getInstance(node.type, params = href, children = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!.children.drop(1).dropLast(1).evaluateChildren(), dri = dri)
                else                                     -> DocNodesFromIElementFactory.getInstance(node.type, params = href, children = listOf(visitNode(linkNode)), dri = dri)
            }
        }

        private fun imagesHandler(node: ASTNode): DocTag {
            val linkNode =
                node.children.last().children.find { it.type == MarkdownElementTypes.LINK_LABEL }!!.children[1]
            val link = text.substring(linkNode.startOffset, linkNode.endOffset)
            val src = mapOf("src" to link)
            return DocNodesFromIElementFactory.getInstance(node.type, params = src, children = listOf(visitNode(node.children.last().children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!)))
        }

        private fun codeSpansHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                children = listOf(
                    DocNodesFromIElementFactory.getInstance(
                        MarkdownTokenTypes.TEXT,
                        body = text.substring(node.startOffset+1, node.endOffset-1).replace('\n', ' ').trimIndent()
                    )

                )
            )

        private fun codeFencesHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                children = node
                    .children
                    .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
                    .map { visitNode(it) },
                params = node
                    .children
                    .find { it.type == MarkdownTokenTypes.FENCE_LANG }
                    ?.let { mapOf("lang" to text.substring(it.startOffset, it.endOffset)) }
                        ?: emptyMap()
            )

        private fun codeBlocksHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(node.type, children = node.children.evaluateChildren())

        private fun defaultHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                MarkdownElementTypes.PARAGRAPH,
                children = node.children.evaluateChildren())

        fun visitNode(node: ASTNode): DocTag =
            when (node.type) {
                MarkdownElementTypes.ATX_1,
                MarkdownElementTypes.ATX_2,
                MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4,
                MarkdownElementTypes.ATX_5,
                MarkdownElementTypes.ATX_6                  -> headersHandler(node)
                MarkdownTokenTypes.HORIZONTAL_RULE          -> horizontalRulesHandler(node)
                MarkdownElementTypes.STRONG,
                MarkdownElementTypes.EMPH                   -> emphasisHandler(node)
                MarkdownElementTypes.FULL_REFERENCE_LINK,
                MarkdownElementTypes.SHORT_REFERENCE_LINK   -> linksHandler(node)
                MarkdownElementTypes.BLOCK_QUOTE            -> blockquotesHandler(node)
                MarkdownElementTypes.UNORDERED_LIST,
                MarkdownElementTypes.ORDERED_LIST           -> listsHandler(node)
                MarkdownElementTypes.CODE_BLOCK             -> codeBlocksHandler(node)
                MarkdownElementTypes.CODE_FENCE             -> codeFencesHandler(node)
                MarkdownElementTypes.CODE_SPAN              -> codeSpansHandler(node)
                MarkdownElementTypes.IMAGE                  -> imagesHandler(node)
                MarkdownTokenTypes.CODE_FENCE_CONTENT,
                MarkdownTokenTypes.CODE_LINE,
                MarkdownTokenTypes.TEXT                     -> DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.TEXT, body = text.substring(node.startOffset, node.endOffset))
                else                                        -> defaultHandler(node)
            }

        private fun List<ASTNode>.evaluateChildren(): List<DocTag> =
            this.filter { it is CompositeASTNode || it.type == MarkdownTokenTypes.TEXT }.map { visitNode(it) }
    }

    private fun markdownToDocNode(text: String): DocTag {

        val flavourDescriptor = CommonMarkFlavourDescriptor()
        val markdownAstRoot: ASTNode = IntellijMarkdownParser(flavourDescriptor).buildMarkdownTreeFromString(text)

        return MarkdownVisitor(text).visitNode(markdownAstRoot)
    }

    override fun parseStringToDocNode(extractedString: String) = markdownToDocNode(extractedString)
    override fun preparse(text: String) = text

    private fun findParent(kDoc: PsiElement): PsiElement =
        if(kDoc is KDocSection) findParent(kDoc.parent) else kDoc

    private fun getAllKDocTags(kDocImpl: PsiElement): List<KDocTag> =
        kDocImpl.children.filterIsInstance<KDocTag>().filterNot { it is KDocSection } + kDocImpl.children.flatMap { getAllKDocTags(it) }

    fun parseFromKDocTag(kDocTag: KDocTag?): DocumentationNode {
        return if(kDocTag == null)
            DocumentationNode(emptyList())
        else
            DocumentationNode(
                (listOf(kDocTag) + getAllKDocTags(findParent(kDocTag) as KDocImpl)).map {
                    when (it.knownTag) {
                        null -> if (it.name == null) Description(parseStringToDocNode(it.getContent())) else CustomWrapperTag(
                            parseStringToDocNode(it.getContent()),
                            it.name!!
                        )
                        KDocKnownTag.AUTHOR -> Author(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.THROWS -> Throws(parseStringToDocNode(it.getContent()), it.getSubjectName().orEmpty())
                        KDocKnownTag.EXCEPTION -> Throws(parseStringToDocNode(it.getContent()), it.getSubjectName().orEmpty())
                        KDocKnownTag.PARAM -> Param(parseStringToDocNode(it.getContent()), it.getSubjectName().orEmpty())
                        KDocKnownTag.RECEIVER -> Receiver(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.RETURN -> Return(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.SEE -> See(parseStringToDocNode(it.getContent()), it.getSubjectName().orEmpty())
                        KDocKnownTag.SINCE -> Since(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.CONSTRUCTOR -> Constructor(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.PROPERTY -> Property(parseStringToDocNode(it.getContent()), it.getSubjectName().orEmpty())
                        KDocKnownTag.SAMPLE -> Sample(parseStringToDocNode(it.getContent()), it.getSubjectName().orEmpty())
                        KDocKnownTag.SUPPRESS -> Suppress(parseStringToDocNode(it.getContent()))
                    }
                }
            )
    }
}