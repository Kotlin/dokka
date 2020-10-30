package org.jetbrains.dokka.base.translators.psi

import com.intellij.psi.*
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.*
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.parsers.factories.DocTagsFromStringFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.tools.projectWizard.core.ParsingState
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): DocumentationNode
}

class JavadocParser(
    private val logger: DokkaLogger // TODO: Add logging
) : JavaDocumentationParser {

    override fun parseDocumentation(element: PsiNamedElement): DocumentationNode {
        val docComment = findClosestDocComment(element) ?: return DocumentationNode(emptyList())
        val nodes = mutableListOf<TagWrapper>()
        docComment.getDescription()?.let { nodes.add(it) }
        nodes.addAll(docComment.tags.mapNotNull { tag ->
            when (tag.name) {
                "param" -> Param(
                    wrapTagIfNecessary(convertJavadocElements(tag.contentElementsWithSiblingIfNeeded().drop(1))),
                    tag.dataElements.firstOrNull()?.text.orEmpty()
                )
                "throws" -> {
                    val resolved = tag.resolveException()
                    val dri = resolved?.let { DRI.from(it) }
                    Throws(
                        root = wrapTagIfNecessary(convertJavadocElements(tag.dataElements.drop(1))),
                        /* we always would like to have a fully qualified name as name,
                        *  because it will be used as a display name later and we would like to have those unified
                        *  even if documentation states shortened version
                        *
                        *  Only if dri search fails we should use the provided phrase (since then we are not able to get a fq name)
                        * */
                        name = resolved?.getKotlinFqName()?.asString()
                            ?: tag.dataElements.firstOrNull()?.text.orEmpty(),
                        exceptionAddress = dri
                    )
                }
                "return" -> Return(wrapTagIfNecessary(convertJavadocElements(tag.contentElementsWithSiblingIfNeeded())))
                "author" -> Author(wrapTagIfNecessary(convertJavadocElements(tag.contentElementsWithSiblingIfNeeded()))) // Workaround: PSI returns first word after @author tag as a `DOC_TAG_VALUE_ELEMENT`, then the rest as a `DOC_COMMENT_DATA`, so for `Name Surname` we get them parted
                "see" -> getSeeTagElementContent(tag).let {
                    See(
                        wrapTagIfNecessary(it.first),
                        tag.referenceElement()?.text.orEmpty(),
                        it.second
                    )
                }
                "deprecated" -> Deprecated(wrapTagIfNecessary(convertJavadocElements(tag.contentElementsWithSiblingIfNeeded())))
                else -> null
            }
        })
        return DocumentationNode(nodes)
    }

    private fun PsiDocTag.resolveException(): PsiElement? =
        dataElements.firstOrNull()?.firstChild?.referenceElementOrSelf()?.resolveToGetDri()

    private fun wrapTagIfNecessary(list: List<DocTag>): CustomDocTag =
        if (list.size == 1 && (list.first() as? CustomDocTag)?.name == MarkdownElementTypes.MARKDOWN_FILE.name)
            list.first() as CustomDocTag
        else
            CustomDocTag(list, name = MarkdownElementTypes.MARKDOWN_FILE.name)

    private fun findClosestDocComment(element: PsiNamedElement): PsiDocComment? {
        (element as? PsiDocCommentOwner)?.docComment?.run { return this }
        if (element is PsiMethod) {
            val superMethods = element.findSuperMethodsOrEmptyArray()
            if (superMethods.isEmpty()) return null

            if (superMethods.size == 1) {
                return findClosestDocComment(superMethods.single())
            }

            val superMethodDocumentation = superMethods.map(::findClosestDocComment)
            if (superMethodDocumentation.size == 1) {
                return superMethodDocumentation.single()
            }

            logger.warn(
                "Conflicting documentation for ${DRI.from(element)}" +
                        "${superMethods.map { DRI.from(it) }}"
            )

            /* Prioritize super class over interface */
            val indexOfSuperClass = superMethods.indexOfFirst { method ->
                val parent = method.parent
                if (parent is PsiClass) !parent.isInterface
                else false
            }

            return if (indexOfSuperClass >= 0) superMethodDocumentation[indexOfSuperClass]
            else superMethodDocumentation.first()
        }
        return element.children.firstIsInstanceOrNull<PsiDocComment>()
    }

    /**
     * Workaround for failing [PsiMethod.findSuperMethods].
     * This might be resolved once ultra light classes are enabled for dokka
     * See [KT-39518](https://youtrack.jetbrains.com/issue/KT-39518)
     */
    private fun PsiMethod.findSuperMethodsOrEmptyArray(): Array<PsiMethod> {
        return try {
            /*
            We are not even attempting to call "findSuperMethods" on all methods called "getGetter" or "getSetter"
            on any object implementing "kotlin.reflect.KProperty", since we know that those methods will fail
            (KT-39518). Just catching the exception is not good enough, since "findSuperMethods" will
            print the whole exception to stderr internally and then spoil the console.
             */
            val kPropertyFqName = FqName("kotlin.reflect.KProperty")
            if (
                this.parent?.safeAs<PsiClass>()?.implementsInterface(kPropertyFqName) == true &&
                (this.name == "getSetter" || this.name == "getGetter")
            ) {
                logger.warn("Skipped lookup of super methods for ${getKotlinFqName()} (KT-39518)")
                return emptyArray()
            }
            findSuperMethods()
        } catch (exception: Throwable) {
            logger.warn("Failed to lookup of super methods for ${getKotlinFqName()} (KT-39518)")
            emptyArray()
        }
    }

    private fun PsiClass.implementsInterface(fqName: FqName): Boolean {
        return allInterfaces().any { it.getKotlinFqName() == fqName }
    }

    private fun PsiClass.allInterfaces(): Sequence<PsiClass> {
        return sequence {
            this.yieldAll(interfaces.toList())
            interfaces.forEach { yieldAll(it.allInterfaces()) }
        }
    }

    private fun getSeeTagElementContent(tag: PsiDocTag): Pair<List<DocumentationLink>, DRI?> {
        val content = tag.referenceElement()?.toDocumentationLink()
        return Pair(listOfNotNull(content), content?.dri)
    }

    private fun PsiDocComment.getDescription(): Description? {
        return convertJavadocElements(descriptionElements.asIterable()).takeIf { it.isNotEmpty() }?.let {
            Description(wrapTagIfNecessary(it))
        }
    }

    private data class ParserState(
        val previousElement: PsiElement? = null,
        val openPreTags: Int = 0,
        val closedPreTags: Int = 0
    )

    private data class ParsingResult(val newState: ParserState = ParserState(), val parsedLine: String? = null) {
        operator fun plus(other: ParsingResult): ParsingResult =
            ParsingResult(
                other.newState,
                listOfNotNull(parsedLine, other.parsedLine).joinToString(separator = "")
            )
    }

    private inner class Parse : (Iterable<PsiElement>, Boolean) -> List<DocTag> {
        val driMap = mutableMapOf<String, DRI>()

        private fun PsiElement.stringify(state: ParserState): ParsingResult =
            when (this) {
                is PsiReference -> children.fold(ParsingResult(state)) { acc, e -> acc + e.stringify(acc.newState) }
                else -> stringifySimpleElement(state)
            }

        private fun PsiElement.stringifySimpleElement(state: ParserState): ParsingResult {
            val openPre = state.openPreTags + "<pre(\\s+.*)?>".toRegex().findAll(text).toList().size
            val closedPre = state.closedPreTags + "</pre>".toRegex().findAll(text).toList().size
            val isInsidePre = openPre > closedPre
            val parsed = when (this) {
                is PsiInlineDocTag -> convertInlineDocTag(this)
                is PsiDocParamRef -> toDocumentationLinkString()
                is PsiDocTagValue,
                is LeafPsiElement -> {
                    if (isInsidePre) {
                        /*
                        For values in the <pre> tag we try to keep formatting, so only the leading space is trimmed,
                        since it is there because it separates this line from the leading asterisk
                         */
                        text.let {
                            if ((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true && it.firstOrNull() == ' ') it.drop(1) else it
                        }.let {
                            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true) it.dropLastWhile { it == ' ' } else it
                        }
                    } else {
                        /*
                        Outside of the <pre> we would like to trim everything from the start and end of a line since
                        javadoc doesn't care about it.
                         */
                        text.let {
                            if ((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true && text != " " && state.previousElement !is PsiInlineDocTag) it?.trimStart() else it
                        }?.let {
                            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true && text != " ") it.trimEnd() else it
                        }?.let {
                            if (shouldHaveSpaceAtTheEnd()) "$it " else it
                        }
                    }
                }
                else -> null
            }
            val previousElement = if (text.trim() == "") state.previousElement else this
            return ParsingResult(
                state.copy(
                    previousElement = previousElement,
                    closedPreTags = closedPre,
                    openPreTags = openPre
                ), parsed
            )
        }

        /**
         * We would like to know if we need to have a space after a this tag
         *
         * The space is required when:
         *  - tag spans multiple lines, between every line we would need a space
         *
         *  We wouldn't like to render a space if:
         *  - tag is followed by an end of comment
         *  - after a tag there is another tag (eg. multiple @author tags)
         *  - they end with an html tag like: <a href="...">Something</a> since then the space will be displayed in the following text
         *  - next line starts with a <p> or <pre> token
         */
        private fun PsiElement.shouldHaveSpaceAtTheEnd(): Boolean {
            val siblings = siblings(withItself = false).toList().filterNot { it.text.trim() == "" }
            val nextNotEmptySibling = (siblings.firstOrNull() as? PsiDocToken)
            val furtherNotEmptySibling =
                (siblings.drop(1).firstOrNull { it is PsiDocToken && !it.isLeadingAsterisk() } as? PsiDocToken)
            val lastHtmlTag = text.trim().substringAfterLast("<")
            val endsWithAnUnclosedTag = lastHtmlTag.endsWith(">") && !lastHtmlTag.startsWith("</")

            return (nextSibling as? PsiWhiteSpace)?.text == "\n " &&
                    (getNextSiblingIgnoringWhitespace() as? PsiDocToken)?.tokenType?.toString() != END_COMMENT_TYPE &&
                    nextNotEmptySibling?.isLeadingAsterisk() == true &&
                    furtherNotEmptySibling?.tokenType?.toString() == COMMENT_TYPE &&
                    !endsWithAnUnclosedTag
        }

        private fun PsiElement.toDocumentationLinkString(
            labelElement: List<PsiElement>? = null
        ): String {
            val label = labelElement?.toList().takeUnless { it.isNullOrEmpty() } ?: listOf(defaultLabel())

            val dri = reference?.resolve()?.takeIf { it !is PsiParameter }?.let {
                val dri = DRI.from(it)
                driMap[dri.toString()] = dri
                dri.toString()
            } ?: UNRESOLVED_PSI_ELEMENT

            return """<a data-dri="$dri">${label.joinToString(" ") { it.text }}</a>"""
        }

        private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.name) {
            "link", "linkplain" -> tag.referenceElement()
                ?.toDocumentationLinkString(tag.dataElements.filterIsInstance<PsiDocToken>())
            "code", "literal" -> "<code data-inline>${tag.text}</code>"
            "index" -> "<index>${tag.children.filterIsInstance<PsiDocTagValue>().joinToString { it.text }}</index>"
            else -> tag.text
        }

        private fun createLink(element: Element, children: List<DocTag>): DocTag {
            return when {
                element.hasAttr("docref") ->
                    A(children, params = mapOf("docref" to element.attr("docref")))
                element.hasAttr("href") ->
                    A(children, params = mapOf("href" to element.attr("href")))
                element.hasAttr("data-dri") && driMap.containsKey(element.attr("data-dri")) ->
                    DocumentationLink(driMap[element.attr("data-dri")]!!, children)
                else -> Text(body = children.filterIsInstance<Text>().joinToString { it.body })
            }
        }

        private fun createBlock(element: Element, insidePre: Boolean = false): DocTag? {
            val children = element.childNodes()
                .mapNotNull { convertHtmlNode(it, insidePre = insidePre || element.tagName() == "pre") }

            fun ifChildrenPresent(operation: () -> DocTag): DocTag? {
                return if (children.isNotEmpty()) operation() else null
            }
            return when (element.tagName()) {
                "blockquote" -> ifChildrenPresent { BlockQuote(children) }
                "p" -> ifChildrenPresent { P(children) }
                "b" -> ifChildrenPresent { B(children) }
                "strong" -> ifChildrenPresent { Strong(children) }
                "index" -> Index(children)
                "i" -> ifChildrenPresent { I(children) }
                "em" -> Em(children)
                "code" -> ifChildrenPresent { CodeInline(children) }
                "pre" -> Pre(children)
                "ul" -> ifChildrenPresent { Ul(children) }
                "ol" -> ifChildrenPresent { Ol(children) }
                "li" -> Li(children)
                "a" -> createLink(element, children)
                "table" -> ifChildrenPresent { Table(children) }
                "tr" -> ifChildrenPresent { Tr(children) }
                "td" -> Td(children)
                "thead" -> THead(children)
                "tbody" -> TBody(children)
                "tfoot" -> TFoot(children)
                "caption" -> ifChildrenPresent { Caption(children) }
                else -> Text(body = element.ownText())
            }
        }

        private fun convertHtmlNode(node: Node, insidePre: Boolean = false): DocTag? = when (node) {
            is TextNode -> (if (insidePre) node.wholeText else node.text()
                .takeIf { it.isNotBlank() })?.let { Text(body = it) }
            is Element -> createBlock(node)
            else -> null
        }

        override fun invoke(elements: Iterable<PsiElement>, asParagraph: Boolean): List<DocTag> =
            elements.fold(ParsingResult()) { acc, e ->
                acc + e.stringify(acc.newState)
            }.parsedLine?.let {
                val trimmed = it.trim()
                val toParse = if (asParagraph) "<p>$trimmed</p>" else trimmed
                Jsoup.parseBodyFragment(toParse).body().childNodes().mapNotNull { convertHtmlNode(it) }
            }.orEmpty()
    }

    private fun PsiDocTag.contentElementsWithSiblingIfNeeded(): List<PsiElement> = if (dataElements.isNotEmpty()) {
        listOfNotNull(
            dataElements[0],
            dataElements[0].nextSibling?.takeIf { it.text != dataElements.drop(1).firstOrNull()?.text },
            *dataElements.drop(1).toTypedArray()
        )
    } else {
        emptyList()
    }

    private fun convertJavadocElements(elements: Iterable<PsiElement>, asParagraph: Boolean = true): List<DocTag> =
        Parse()(elements, asParagraph)

    private fun PsiDocToken.isSharpToken() = tokenType.toString() == "DOC_TAG_VALUE_SHARP_TOKEN"

    private fun PsiDocToken.isLeadingAsterisk() = tokenType.toString() == "DOC_COMMENT_LEADING_ASTERISKS"

    private fun PsiElement.toDocumentationLink(labelElement: PsiElement? = null) =
        resolveToGetDri()?.let {
            val dri = DRI.from(it)
            val label = labelElement ?: defaultLabel()
            DocumentationLink(dri, convertJavadocElements(listOfNotNull(label), asParagraph = false))
        }

    private fun PsiElement.resolveToGetDri(): PsiElement? =
        reference?.resolve()

    private fun PsiDocTag.referenceElement(): PsiElement? =
        linkElement()?.referenceElementOrSelf()

    private fun PsiElement.referenceElementOrSelf(): PsiElement? =
        if (node.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER) {
            PsiTreeUtil.findChildOfType(this, PsiJavaCodeReferenceElement::class.java)
        } else this

    private fun PsiElement.defaultLabel() = children.firstOrNull {
        it is PsiDocToken && it.text.isNotBlank() && !it.isSharpToken()
    } ?: this

    private fun PsiDocTag.linkElement(): PsiElement? =
        valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

    companion object {
        private const val UNRESOLVED_PSI_ELEMENT = "UNRESOLVED_PSI_ELEMENT"
        private const val END_COMMENT_TYPE = "DOC_COMMENT_END"
        private const val COMMENT_TYPE = "DOC_COMMENT_DATA"
    }
}
