package org.jetbrains.dokka

import com.intellij.psi.*
import com.intellij.psi.impl.source.javadoc.CorePsiDocTagValueImpl
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocTagValue
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.regex.Pattern

private val REF_COMMAND = "ref"
private val NAME_COMMAND = "name"
private val DESCRIPTION_COMMAND = "description"
private val NAME_TEXT = Pattern.compile("(\\S+)(.*)", Pattern.DOTALL)

data class JavadocParseResult(
        val content: Content,
        val deprecatedContent: Content?,
        val attributes: List<DocumentationNode>,
        val apiLevel: DocumentationNode?
) {
    companion object {
        val Empty = JavadocParseResult(Content.Empty, null, emptyList(), null)
    }
}

interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): JavadocParseResult
}

class JavadocParser(
    private val refGraph: NodeReferenceGraph,
    private val logger: DokkaLogger,
    private val signatureProvider: ElementSignatureProvider
) : JavaDocumentationParser {
    override fun parseDocumentation(element: PsiNamedElement): JavadocParseResult {
        val docComment = (element as? PsiDocCommentOwner)?.docComment
        if (docComment == null) return JavadocParseResult.Empty
        val result = MutableContent()
        var deprecatedContent: Content? = null
        val firstParagraph = ContentParagraph()
        firstParagraph.convertJavadocElements(docComment.descriptionElements.dropWhile { it.text.trim().isEmpty() })
        val paragraphs = firstParagraph.children.dropWhile { it !is ContentParagraph }
        firstParagraph.children.removeAll(paragraphs)
        if (!firstParagraph.isEmpty()) {
            result.append(firstParagraph)
        }
        paragraphs.forEach {
            result.append(it)
        }
        val attrs = mutableListOf<DocumentationNode>()
        var since: DocumentationNode? = null
        docComment.tags.forEach { tag ->
            when (tag.name) {
                "see" -> result.convertSeeTag(tag)
                "deprecated" -> {
                    deprecatedContent = Content().apply {
                        convertJavadocElements(tag.contentElements())
                    }
                }
                "attr" -> {
                    tag.getAttr(element)?.let { attrs.add(it) }
                }
                "since" -> {
                    since = DocumentationNode(tag.minApiLevel() ?: "", Content.Empty, NodeKind.ApiLevel)
                }
                else -> {
                    val subjectName = tag.getSubjectName()
                    val section = result.addSection(javadocSectionDisplayName(tag.name), subjectName)
                    section.convertJavadocElements(tag.contentElements())
                }
            }
        }
        return JavadocParseResult(result, deprecatedContent, attrs, since)
    }

    fun PsiDocTag.minApiLevel(): String? {
        if (dataElements.isNotEmpty()) {
            val data = dataElements
            if (data[0] is CorePsiDocTagValueImpl) {
                val docTagValue = data[0]
                if (docTagValue.firstChild != null) {
                    val apiLevel = docTagValue.firstChild
                    return apiLevel.text
                }
            }
        }
        return null
    }

    private fun PsiDocTag.getAttr(element: PsiNamedElement): DocumentationNode? = when (valueElement?.text) {
            REF_COMMAND -> {
                if (dataElements.size > 1) {
                    val elementText = dataElements[1].text
                    val names = elementText.split("#")
                    if (names.size > 1) {
                        val qualifiedAttribute = names[1].split("_")
                        if (qualifiedAttribute.size > 1) {
                            val attribute = qualifiedAttribute[1]
                            val attrRef = "android.R#" + attribute
                                    try {
                                        val linkComment = JavaPsiFacade.getInstance(project).elementFactory
                                                .createDocCommentFromText("/** {@link $attrRef} */", element)
                                        if (attrRef.contains("cacheColorHint")) {
                                            val x = false
                                        }
                                        val linkElement = PsiTreeUtil.getChildOfType(linkComment, PsiInlineDocTag::class.java)?.linkElement()
                                        val link = resolveLink(linkElement)
                                        if (link != null) {
                                            DocumentationNode(attrRef, Content.Empty, NodeKind.Attribute).also {
                                                refGraph.link(it, link, RefKind.Attribute)
                                            }
                                        } else null
                                    } catch (e: IncorrectOperationException) {
                                        null
                                    }
                        } else null
                    } else null
                } else null
            }
            NAME_COMMAND -> {
                if (dataElements.size > 1) {
                    val nameMatcher = NAME_TEXT.matcher(dataElements[1].text)
                    if (nameMatcher.matches()) {
                        val attrName = nameMatcher.group(1)
                        DocumentationNode(attrName, Content.Empty, NodeKind.Attribute)
                    } else {
                        null
                    }
                } else null
            }
            DESCRIPTION_COMMAND -> {
                val attrDescription = dataElements.toString()
                DocumentationNode(attrDescription, Content.Empty, NodeKind.Attribute)
            }
            else -> null
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
            convertHtmlNode(it)?.let { append(it) }
        }
    }

    private fun convertHtmlNode(node: Node): ContentNode? {
        if (node is TextNode) {
            return ContentText(node.text())
        } else if (node is Element) {
            val childBlock = createBlock(node)
            node.childNodes().forEach {
                val child = convertHtmlNode(it)
                if (child != null) {
                    childBlock.append(child)
                }
            }
            return (childBlock)
        }
        return null
    }

    private fun createBlock(element: Element): ContentBlock = when (element.tagName()) {
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
        if (element.hasAttr("docref")) {
            val docref = element.attr("docref")
            return ContentNodeLazyLink(docref, { -> refGraph.lookupOrWarn(docref, logger) })
        }
        return if (element.hasAttr("href")) {
            val href = element.attr("href")
            ContentExternalLink(href)
        } else {
            ContentBlock()
        }
    }

    private fun MutableContent.convertSeeTag(tag: PsiDocTag) {
        val linkElement = tag.linkElement() ?: return
        val seeSection = findSectionByTag(ContentTags.SeeAlso) ?: addSection(ContentTags.SeeAlso, null)
        val linkSignature = resolveLink(tag.referenceElement())
        val text = ContentText(linkElement.text)
        if (linkSignature != null) {
            val linkNode =
                ContentNodeLazyLink((tag.valueElement ?: linkElement).text, { -> refGraph.lookupOrWarn(linkSignature, logger) })
            linkNode.append(text)
            seeSection.append(linkNode)
        } else {
            seeSection.append(text)
        }
    }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.name) {
        "link", "linkplain" -> {
            val valueElement = tag.referenceElement()
            val linkSignature = resolveLink(valueElement)
            if (linkSignature != null) {
                val labelText = tag.dataElements.firstOrNull { it is PsiDocToken }?.text ?: valueElement!!.text
                val link = "<a docref=\"$linkSignature\">${labelText.htmlEscape()}</a>"
                if (tag.name == "link") "<code>$link</code>" else link
            } else if (valueElement != null) {
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

    private fun PsiDocTag.referenceElement(): PsiElement? =
            linkElement()?.let {
                if (it.node.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER) {
                    PsiTreeUtil.findChildOfType(it, PsiJavaCodeReferenceElement::class.java)
                } else {
                    it
                }
            }

    private fun PsiDocTag.linkElement(): PsiElement? =
        valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

    private fun resolveLink(valueElement: PsiElement?): String? {
        val target = valueElement?.reference?.resolve()
        if (target != null) {
            return signatureProvider.signature(target)
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
