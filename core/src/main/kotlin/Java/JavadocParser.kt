package org.jetbrains.dokka

import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocTagValue
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

data class JavadocParseResult(val content: Content, val deprecatedContent: Content?) {
    companion object {
        val Empty = JavadocParseResult(Content.Empty, null)
    }
}

interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): JavadocParseResult
}

class JavadocParser(private val refGraph: NodeReferenceGraph,
                    private val logger: DokkaLogger) : JavaDocumentationParser {
    override fun parseDocumentation(element: PsiNamedElement): JavadocParseResult {
        val docComment = (element as? PsiDocCommentOwner)?.docComment
        if (docComment == null) return JavadocParseResult.Empty
        val result = MutableContent()
        var deprecatedContent: Content? = null
        val para = ContentParagraph()
        result.append(para)
        para.convertJavadocElements(docComment.descriptionElements.dropWhile { it.text.trim().isEmpty() })
        docComment.tags.forEach { tag ->
            when(tag.name) {
                "see" -> result.convertSeeTag(tag)
                "deprecated" -> {
                    deprecatedContent = Content()
                    deprecatedContent!!.convertJavadocElements(tag.contentElements())
                }
                else -> {
                    val subjectName = tag.getSubjectName()
                    val section = result.addSection(javadocSectionDisplayName(tag.name), subjectName)

                    section.convertJavadocElements(tag.contentElements())
                }
            }
        }
        return JavadocParseResult(result, deprecatedContent)
    }

    private fun PsiDocTag.contentElements(): Iterable<PsiElement> {
        val tagValueElements = children
                .dropWhile { it.node?.elementType == JavaDocTokenType.DOC_TAG_NAME }
                .dropWhile { it is PsiWhiteSpace }
                .filterNot { it.node?.elementType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS }
        return if (getSubjectName() != null) tagValueElements.dropWhile { it is PsiDocTagValue } else tagValueElements
    }

    private fun ContentBlock.convertJavadocElements(elements: Iterable<PsiElement>) {
        val htmlBuilder = StringBuilder()
        elements.forEach {
            if (it is PsiInlineDocTag) {
                htmlBuilder.append(convertInlineDocTag(it))
            } else {
                htmlBuilder.append(it.text)
            }
        }
        val doc = Jsoup.parse(htmlBuilder.toString().trim())
        doc.body().childNodes().forEach {
            convertHtmlNode(it)
        }
    }

    private fun ContentBlock.convertHtmlNode(node: Node) {
        if (node is TextNode) {
            append(ContentText(node.text()))
        } else if (node is Element) {
            val childBlock = createBlock(node)
            node.childNodes().forEach {
                childBlock.convertHtmlNode(it)
            }
            append(childBlock)
        }
    }

    private fun createBlock(element: Element): ContentBlock = when(element.tagName()) {
        "p" -> ContentParagraph()
        "b", "strong" -> ContentStrong()
        "i", "em" -> ContentEmphasis()
        "s", "del" -> ContentStrikethrough()
        "code" -> ContentCode()
        "pre" -> ContentBlockCode()
        "ul" -> ContentUnorderedList()
        "ol" -> ContentOrderedList()
        "li" -> ContentListItem()
        "a" -> createLink(element)
        "br" -> ContentBlock().apply { hardLineBreak() }
        else -> ContentBlock()
    }

    private fun createLink(element: Element): ContentBlock {
        val docref = element.attr("docref")
        if (docref != null) {
            return ContentNodeLazyLink(docref, { -> refGraph.lookupOrWarn(docref, logger)})
        }
        val href = element.attr("href")
        if (href != null) {
            return ContentExternalLink(href)
        } else {
            return ContentBlock()
        }
    }

    private fun MutableContent.convertSeeTag(tag: PsiDocTag) {
        val linkElement = tag.linkElement()
        if (linkElement == null) {
            return
        }
        val seeSection = findSectionByTag(ContentTags.SeeAlso) ?: addSection(ContentTags.SeeAlso, null)
        val linkSignature = resolveLink(linkElement)
        val text = ContentText(linkElement.text)
        if (linkSignature != null) {
            val linkNode = ContentNodeLazyLink(tag.valueElement!!.text, { -> refGraph.lookupOrWarn(linkSignature, logger)})
            linkNode.append(text)
            seeSection.append(linkNode)
        } else {
            seeSection.append(text)
        }
    }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.name) {
        "link", "linkplain" -> {
            val valueElement = tag.linkElement()
            val linkSignature = resolveLink(valueElement)
            if (linkSignature != null) {
                val labelText = tag.dataElements.firstOrNull { it is PsiDocToken }?.text ?: valueElement!!.text
                val link = "<a docref=\"$linkSignature\">${labelText.htmlEscape()}</a>"
                if (tag.name == "link") "<code>$link</code>" else link
            }
            else if (valueElement != null) {
                valueElement.text
            } else {
                ""
            }
        }
        "code", "literal" -> {
            val text = StringBuilder()
            tag.dataElements.forEach { text.append(it.text) }
            val escaped = text.toString().trimStart().htmlEscape()
            if (tag.name == "code") "<code>$escaped</code>" else escaped
        }
        else -> tag.text
    }

    private fun PsiDocTag.linkElement(): PsiElement? =
            valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

    private fun resolveLink(valueElement: PsiElement?): String? {
        val target = valueElement?.reference?.resolve()
        if (target != null) {
            return getSignature(target)
        }
        return null
    }

    fun PsiDocTag.getSubjectName(): String? {
        if (name == "param" || name == "throws" || name == "exception") {
            return valueElement?.text
        }
        return null
    }
}
