package org.jetbrains.dokka

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.javadoc.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): DocumentationNode
}

class JavadocParser(
    private val logger: DokkaLogger
) : JavaDocumentationParser {

    override fun parseDocumentation(element: PsiNamedElement): DocumentationNode {
        val docComment = (element as? PsiDocCommentOwner)?.docComment ?: return DocumentationNode(emptyList())
        val nodes =
            convertJavadocElements(docComment.descriptionElements.dropWhile { it.text.trim().isEmpty() }, element)
        return DocumentationNode(nodes.map { Description(it) })
        /*val firstParagraphContents = nodes.takeWhile { it !is ContentParagraph }
        val firstParagraph = ContentParagraph()
        if (firstParagraphContents.isNotEmpty()) {
            firstParagraphContents.forEach { firstParagraph.append(it) }
            result.add(firstParagraph)
        }

        result.appendAll(nodes.drop(firstParagraphContents.size))

        if (element is PsiMethod) {
            val tagsByName = element.searchInheritedTags()
            for ((tagName, tags) in tagsByName) {
                for ((tag, context) in tags) {
                    val section = result.addSection(javadocSectionDisplayName(tagName), tag.getSubjectName())
                    val signature = signatureProvider.signature(element)
                    when (tagName) {
                        "param" -> {
                            section.appendTypeElement(signature) {
                                it.details
                                    .find { node -> node.kind == NodeKind.Parameter && node.name == tag.getSubjectName() }
                                    ?.detailOrNull(NodeKind.Type)
                            }
                        }
                        "return" -> {
                            section.appendTypeElement(signature) { it.detailOrNull(NodeKind.Type) }
                        }
                    }
                    section.appendAll(convertJavadocElements(tag.contentElements(), context))
                }
            }
        }

        docComment.tags.forEach { tag ->
            when (tag.name) {
                "see" -> result.convertSeeTag(tag)
                "deprecated" -> {
                    deprecatedContent = Content().apply {
                        appendAll(convertJavadocElements(tag.contentElements(), element))
                    }
                }
                in tagsToInherit -> {}
                else -> {
                    val subjectName = tag.getSubjectName()
                    val section = result.addSection(javadocSectionDisplayName(tag.name), subjectName)

                    section.appendAll(convertJavadocElements(tag.contentElements(), element))
                }
            }
        }
        return JavadocParseResult(result, deprecatedContent)*/
    }

    private val tagsToInherit = setOf("param", "return", "throws")

    private data class TagWithContext(val tag: PsiDocTag, val context: PsiNamedElement)
/*
    private fun PsiMethod.searchInheritedTags(): Map<String, Collection<TagWithContext>> {

        val output = tagsToInherit.keysToMap { mutableMapOf<String?, TagWithContext>() }

        fun recursiveSearch(methods: Array<PsiMethod>) {
            for (method in methods) {
                recursiveSearch(method.findSuperMethods())
            }
            for (method in methods) {
                for (tag in method.docComment?.tags.orEmpty()) {
                    if (tag.name in tagsToInherit) {
                        output[tag.name]!![tag.getSubjectName()] = TagWithContext(tag, method)
                    }
                }
            }
        }

        recursiveSearch(arrayOf(this))
        return output.mapValues { it.value.values }
    }
*/

    private fun PsiDocTag.contentElements(): Iterable<PsiElement> {
        val tagValueElements = children
            .dropWhile { it.node?.elementType == JavaDocTokenType.DOC_TAG_NAME }
            .dropWhile { it is PsiWhiteSpace }
            .filterNot { it.node?.elementType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS }
        return if (getSubjectName() != null) tagValueElements.dropWhile { it is PsiDocTagValue } else tagValueElements
    }

    private fun convertJavadocElements(elements: Iterable<PsiElement>, element: PsiNamedElement): List<DocTag> {
        val doc = Jsoup.parse(expandAllForElements(elements, element))
        return doc.body().childNodes().mapNotNull { convertHtmlNode(it) }
    }

    private fun expandAllForElements(elements: Iterable<PsiElement>, element: PsiNamedElement): String {
        val htmlBuilder = StringBuilder()
        elements.forEach {
            if (it is PsiInlineDocTag) {
                htmlBuilder.append(convertInlineDocTag(it, element))
            } else {
                htmlBuilder.append(it.text)
            }
        }
        return htmlBuilder.toString().trim()
    }

    private fun convertHtmlNode(node: Node, insidePre: Boolean = false): DocTag? = when (node) {
        is TextNode -> Text(body = if (insidePre) node.wholeText else node.text())
        is Element -> createBlock(node)
        else -> null
    }

    private fun createBlock(element: Element): DocTag {
        val children = element.childNodes().mapNotNull { convertHtmlNode(it) }
        return when (element.tagName()) {
            "p" -> P(children)
            "b" -> B(children)
            "strong" -> Strong(children)
            "i" -> I(children)
            "em" -> Em(children)
//            "s", "del" -> ContentStrikethrough()
            "code" -> Code(children)
            "pre" -> Pre(children)
            "ul" -> Ul(children)
            "ol" -> Ol(children)
            "li" -> Li(children)
//            "a" -> createLink(element)
//            "br" -> ContentBlock().apply { hardLineBreak() }
            else -> Text(body = element.ownText())
        }
    }
/*

    private fun createLink(element: Element): DocTag {
        return when {
            element.hasAttr("docref") -> {
                val docref = element.attr("docref")
                ContentNodeLazyLink(docref) { refGraph.lookupOrWarn(docref, logger) }
            }
            element.hasAttr("href") -> {
                val href = element.attr("href")

                val uri = try {
                    URI(href)
                } catch (_: Exception) {
                    null
                }

                if (uri?.isAbsolute == false) {
                    ContentLocalLink(href)
                } else {
                    ContentExternalLink(href)
                }
            }
            element.hasAttr("name") -> {
                ContentBookmark(element.attr("name"))
            }
            else -> ContentBlock()
        }
    }


    private fun convertSeeTag(tag: PsiDocTag) {
        val linkElement = tag.linkElement() ?: return
        val seeSection = findSectionByTag(ContentTags.SeeAlso) ?: addSection(ContentTags.SeeAlso, null)

        val valueElement = tag.referenceElement()
        val externalLink = resolveExternalLink(valueElement)
        val text = ContentText(linkElement.text)

        val linkSignature by lazy { resolveInternalLink(valueElement) }
        val node = when {
            externalLink != null -> {
                val linkNode = ContentExternalLink(externalLink)
                linkNode.append(text)
                linkNode
            }
            linkSignature != null -> {
                val linkNode =
                        ContentNodeLazyLink(
                                (tag.valueElement ?: linkElement).text
                        ) { refGraph.lookupOrWarn(linkSignature!!, logger) }
                linkNode.append(text)
                linkNode
            }
            else -> text
        }
        seeSection.append(node)
    }
*/
    private fun convertInlineDocTag(tag: PsiInlineDocTag, element: PsiNamedElement) = when (tag.name) {
        "link", "linkplain" -> {
            val valueElement = tag.referenceElement()
            val externalLink = resolveExternalLink(valueElement)
            val linkSignature by lazy { resolveInternalLink(valueElement) }
            if (externalLink != null || linkSignature != null) {
                val labelText = tag.dataElements.firstOrNull { it is PsiDocToken }?.text ?: valueElement!!.text
                val linkTarget = if (externalLink != null) "href=\"$externalLink\"" else "docref=\"$linkSignature\""
                val link = "<a $linkTarget>$labelText</a>"
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
            val escaped = text.toString().trimStart()
            if (tag.name == "code") "<code>$escaped</code>" else escaped
        }
        "inheritDoc" -> {
            val result = (element as? PsiMethod)?.let {
                // @{inheritDoc} is only allowed on functions
                val parent = tag.parent
                when (parent) {
                    is PsiDocComment -> element.findSuperDocCommentOrWarn()
                    is PsiDocTag -> element.findSuperDocTagOrWarn(parent)
                    else -> null
                }
            }
            result ?: tag.text
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

    private fun resolveExternalLink(valueElement: PsiElement?): String? {
        /*val target = valueElement?.reference?.resolve()
        if (target != null) {
            return externalDocumentationLinkResolver.buildExternalDocumentationLink(target)
        }*/
        return null
    }

    private fun resolveInternalLink(valueElement: PsiElement?): String? {
        /*val target = valueElement?.reference?.resolve()
        if (target != null) {
            return signatureProvider.signature(target)
        }*/
        return null
    }

    fun PsiDocTag.getSubjectName(): String? {
        if (name == "param" || name == "throws" || name == "exception") {
            return valueElement?.text
        }
        return null
    }

    private fun PsiMethod.findSuperDocCommentOrWarn(): String {
        val method = findFirstSuperMethodWithDocumentation(this)
        if (method != null) {
            val descriptionElements = method.docComment?.descriptionElements?.dropWhile {
                it.text.trim().isEmpty()
            } ?: return ""

            return expandAllForElements(descriptionElements, method)
        }
        logger.warn("No docs found on supertype with {@inheritDoc} method ${this.name} in ${this.containingFile.name}}")
        return ""
    }


    private fun PsiMethod.findSuperDocTagOrWarn(elementToExpand: PsiDocTag): String {
        val result = findFirstSuperMethodWithDocumentationforTag(elementToExpand, this)

        if (result != null) {
            val (method, tag) = result

            val contentElements = tag.contentElements().dropWhile { it.text.trim().isEmpty() }

            val expandedString = expandAllForElements(contentElements, method)

            return expandedString
        }
        logger.warn("No docs found on supertype for @${elementToExpand.name} ${elementToExpand.getSubjectName()} with {@inheritDoc} method ${this.name} in ${this.containingFile.name}}")
        return ""
    }

    private fun findFirstSuperMethodWithDocumentation(current: PsiMethod): PsiMethod? {
        val superMethods = current.findSuperMethods()
        for (method in superMethods) {
            val docs = method.docComment?.descriptionElements?.dropWhile { it.text.trim().isEmpty() }
            if (!docs.isNullOrEmpty()) {
                return method
            }
        }
        for (method in superMethods) {
            val result = findFirstSuperMethodWithDocumentation(method)
            if (result != null) {
                return result
            }
        }

        return null
    }

    private fun findFirstSuperMethodWithDocumentationforTag(elementToExpand: PsiDocTag, current: PsiMethod): Pair<PsiMethod, PsiDocTag>? {
        val superMethods = current.findSuperMethods()
        val mappedFilteredTags = superMethods.map {
            it to it.docComment?.tags?.filter { it.name == elementToExpand.name }
        }

        for ((method, tags) in mappedFilteredTags) {
            tags ?: continue
            for (tag in tags) {
                val (tagSubject, elementSubject) = when (tag.name) {
                    "throws" -> {
                        // match class names only for throws, ignore possibly fully qualified path
                        // TODO: Always match exactly here
                        tag.getSubjectName()?.split(".")?.last() to elementToExpand.getSubjectName()?.split(".")?.last()
                    }
                    else -> {
                        tag.getSubjectName() to elementToExpand.getSubjectName()
                    }
                }

                if (tagSubject == elementSubject) {
                    return method to tag
                }
            }
        }

        for (method in superMethods) {
            val result = findFirstSuperMethodWithDocumentationforTag(elementToExpand, method)
            if (result != null) {
                return result
            }
        }
        return null
    }

}
