package org.jetbrains.dokka.parsers

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.model.doc.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.impl.ListItemCompositeNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.parsers.factories.DocNodesFromIElementFactory
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import java.net.MalformedURLException
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

class MarkdownParser (
    private val resolutionFacade: DokkaResolutionFacade,
    private val declarationDescriptor: DeclarationDescriptor
    ) : Parser() {

    inner class MarkdownVisitor(val text: String, val destinationLinksMap: Map<String, String>) {

        private fun headersHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                visitNode(node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT } ?:
                throw Error("Wrong AST Tree. ATX Header does not contain expected content")).children
            )

        private fun horizontalRulesHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.HORIZONTAL_RULE)

        private fun emphasisHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                children = listOf(visitNode(node.children[node.children.size / 2]))
            )

        private fun blockquotesHandler(node: ASTNode): DocTag =
            DocNodesFromIElementFactory.getInstance(node.type, children = node.children
                .filterIsInstance<CompositeASTNode>().evaluateChildren())

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
                                    .filterIsInstance<CompositeASTNode>()
                                    .evaluateChildren()
                            )
                        else
                            visitNode(it)
                    },
                params =
                if (node.type == MarkdownElementTypes.ORDERED_LIST) {
                    val listNumberNode = node.children.first().children.first()
                    mapOf("start" to text.substring(listNumberNode.startOffset, listNumberNode.endOffset).trim().dropLast(1))
                } else
                    emptyMap()
            )
        }

        private fun resolveDRI(link: String): DRI? =
            try {
                java.net.URL(link)
                null
            } catch(e: MalformedURLException) {
                resolveKDocLink(
                    resolutionFacade.resolveSession.bindingContext,
                    resolutionFacade,
                    declarationDescriptor,
                    null,
                    link.split('.')
                ).also { if (it.size > 1) throw Error("Markdown link resolved more than one element: $it") }.firstOrNull()//.single()
                    ?.let { DRI.from(it) }
            }

        private fun referenceLinksHandler(node: ASTNode): DocTag {
            val linkLabel = node.children.find { it.type == MarkdownElementTypes.LINK_LABEL }  ?:
                throw Error("Wrong AST Tree. Reference link does not contain expected content")
            val linkText = node.children.findLast { it.type == MarkdownElementTypes.LINK_TEXT } ?: linkLabel

            val linkKey = text.substring(linkLabel.startOffset, linkLabel.endOffset)

            val link = destinationLinksMap[linkKey.toLowerCase()] ?: linkKey

            return linksHandler(linkText, link)
        }

        private fun inlineLinksHandler(node: ASTNode): DocTag {
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }  ?:
                throw Error("Wrong AST Tree. Inline link does not contain expected content")
            val linkDestination = node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }  ?:
                throw Error("Wrong AST Tree. Inline link does not contain expected content")
            val linkTitle = node.children.find { it.type == MarkdownElementTypes.LINK_TITLE }

            val link = text.substring(linkDestination.startOffset, linkDestination.endOffset)

            return linksHandler(linkText, link, linkTitle)
        }

        private fun autoLinksHandler(node: ASTNode): DocTag {
            val link = text.substring(node.startOffset + 1, node.endOffset - 1)

            return linksHandler(node, link)
        }

        private fun linksHandler(linkText: ASTNode, link: String, linkTitle: ASTNode? = null): DocTag {
            val dri: DRI? = resolveDRI(link)
            val params = if(linkTitle == null)
                mapOf("href" to link)
            else
                mapOf("href" to link, "title" to text.substring(linkTitle.startOffset + 1, linkTitle.endOffset - 1))

            return DocNodesFromIElementFactory.getInstance(MarkdownElementTypes.INLINE_LINK, params = params, children = linkText.children.drop(1).dropLast(1).evaluateChildren(), dri = dri)
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
                    .let { listOf(Text( body = text.substring(
                            it.find { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }?.startOffset ?: 0,
                            it.findLast { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }?.endOffset ?: 0 //TODO: Problem with empty code fence
                        ))
                    ) },
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
                MarkdownElementTypes.SHORT_REFERENCE_LINK   -> referenceLinksHandler(node)
                MarkdownElementTypes.INLINE_LINK            -> inlineLinksHandler(node)
                MarkdownElementTypes.AUTOLINK               -> autoLinksHandler(node)
                MarkdownElementTypes.BLOCK_QUOTE            -> blockquotesHandler(node)
                MarkdownElementTypes.UNORDERED_LIST,
                MarkdownElementTypes.ORDERED_LIST           -> listsHandler(node)
                MarkdownElementTypes.CODE_BLOCK             -> codeBlocksHandler(node)
                MarkdownElementTypes.CODE_FENCE             -> codeFencesHandler(node)
                MarkdownElementTypes.CODE_SPAN              -> codeSpansHandler(node)
                MarkdownElementTypes.IMAGE                  -> imagesHandler(node)
                MarkdownTokenTypes.CODE_FENCE_CONTENT,
                MarkdownTokenTypes.CODE_LINE,
                MarkdownTokenTypes.TEXT                     -> DocNodesFromIElementFactory.getInstance(
                    MarkdownTokenTypes.TEXT,
                    body = text
                        .substring(node.startOffset, node.endOffset).transform()
                )
                MarkdownElementTypes.MARKDOWN_FILE          -> if(node.children.size == 1) visitNode(node.children.first()) else defaultHandler(node)
                else                                        -> defaultHandler(node)
            }

        private fun List<ASTNode>.evaluateChildren(): List<DocTag> =
            this.removeUselessTokens().mergeLeafASTNodes().map { visitNode(it) }

        private fun List<ASTNode>.removeUselessTokens(): List<ASTNode> =
            this.filterIndexed { index, _ ->
                !(this[index].type == MarkdownTokenTypes.EOL &&
                this.isLeaf(index - 1) && this.getOrNull(index - 1)?.type !in leafNodes &&
                this.isLeaf(index + 1) && this.getOrNull(index + 1)?.type !in leafNodes) &&
                this[index].type != MarkdownElementTypes.LINK_DEFINITION
            }

        private val notLeafNodes = listOf(MarkdownTokenTypes.HORIZONTAL_RULE)
        private val leafNodes = listOf(MarkdownElementTypes.STRONG, MarkdownElementTypes.EMPH)

        private fun List<ASTNode>.isLeaf(index: Int): Boolean =
            if(index in 0..this.lastIndex)
                (this[index] is CompositeASTNode)|| this[index].type in notLeafNodes
            else
                false

        private fun List<ASTNode>.mergeLeafASTNodes(): List<ASTNode> {
            val children: MutableList<ASTNode> = mutableListOf()
            var index = 0
            while(index <= this.lastIndex) {
                if(this.isLeaf(index)) {
                    children += this[index]
                }
                else {
                    val startOffset = this[index].startOffset
                    while(index < this.lastIndex) {
                        if(this.isLeaf(index + 1)) {
                            val endOffset = this[index].endOffset
                            if(text.substring(startOffset, endOffset).transform().isNotEmpty())
                                children += LeafASTNode(MarkdownTokenTypes.TEXT, startOffset, endOffset)
                            break
                        }
                        index++
                    }
                    if(index == this.lastIndex) {
                        val endOffset = this[index].endOffset
                        if(text.substring(startOffset, endOffset).transform().isNotEmpty())
                            children += LeafASTNode(MarkdownTokenTypes.TEXT, startOffset, endOffset)
                    }
                }
                index++
            }
            return children
        }

        private fun String.transform() = this
            .replace(Regex("\n\n+"), "")        // Squashing new lines between paragraphs
            .replace(Regex("\n>+ "), "\n")      // Replacement used in blockquotes, get rid of garbage
    }


    private fun getAllDestinationLinks(text: String, node: ASTNode): List<Pair<String, String>> =
    node.children
        .filter { it.type == MarkdownElementTypes.LINK_DEFINITION }
        .map { text.substring(it.children[0].startOffset, it.children[0].endOffset).toLowerCase() to
               text.substring(it.children[2].startOffset, it.children[2].endOffset) } +
            node.children.filterIsInstance<CompositeASTNode>().flatMap { getAllDestinationLinks(text, it) }


    private fun markdownToDocNode(text: String): DocTag {

        val flavourDescriptor = CommonMarkFlavourDescriptor()
        val markdownAstRoot: ASTNode = IntellijMarkdownParser(flavourDescriptor).buildMarkdownTreeFromString(text)

        return MarkdownVisitor(text, getAllDestinationLinks(text, markdownAstRoot).toMap()).visitNode(markdownAstRoot)
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