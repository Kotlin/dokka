package org.jetbrains.dokka.base.translators.psi.parsers

import com.intellij.lexer.JavaDocTokenTypes
import com.intellij.psi.*
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.lexer.Compat.forEachCodePoint
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.translators.parseHtmlEncodedWithNormalisedSpaces
import org.jetbrains.dokka.base.translators.parseWithNormalisedSpaces
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.enumValueOrNull
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.util.CommentSaver.Companion.tokenType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.*

interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): DocumentationNode
}

class JavadocParser(
    private val logger: DokkaLogger,
    private val resolutionFacade: DokkaResolutionFacade,
) : JavaDocumentationParser {
    private val inheritDocResolver = InheritDocResolver(logger)

    /**
     * Cache created to make storing entries from kotlin easier.
     *
     * It has to be mutable to allow for adding entries when @inheritDoc resolves to kotlin code,
     * from which we get a DocTags not descriptors.
     */
    private var inheritDocSections: MutableMap<UUID, DocumentationNode> = mutableMapOf()

    override fun parseDocumentation(element: PsiNamedElement): DocumentationNode {
        return when(val comment = findClosestDocComment(element, logger)){
            is JavaDocComment -> parseDocumentation(comment, element)
            is KotlinDocComment -> parseDocumentation(comment)
            else -> DocumentationNode(emptyList())
        }
    }

    private fun parseDocumentation(element: JavaDocComment, context: PsiNamedElement): DocumentationNode {
        val docComment = element.comment
        val nodes = listOfNotNull(docComment.getDescription()) + docComment.tags.mapNotNull { tag ->
            parseDocTag(tag, docComment, context)
        }
        return DocumentationNode(nodes)
    }

    private fun parseDocumentation(element: KotlinDocComment, parseWithChildren: Boolean = true): DocumentationNode =
        MarkdownParser.parseFromKDocTag(
            kDocTag = element.comment,
            externalDri = { link: String ->
                try {
                    resolveKDocLink(
                        context = resolutionFacade.resolveSession.bindingContext,
                        resolutionFacade = resolutionFacade,
                        fromDescriptor = element.descriptor,
                        fromSubjectOfTag = null,
                        qualifiedName = link.split('.')
                    ).firstOrNull()?.let { DRI.from(it) }
                } catch (e1: IllegalArgumentException) {
                    logger.warn("Couldn't resolve link for $link")
                    null
                }
            },
            kdocLocation = null,
            parseWithChildren = parseWithChildren
        )

    private fun parseDocTag(tag: PsiDocTag, docComment: PsiDocComment, analysedElement: PsiNamedElement): TagWrapper? =
        enumValueOrNull<JavadocTag>(tag.name)?.let { javadocTag ->
            val resolutionContext = CommentResolutionContext(comment = docComment, tag = javadocTag)
            when (resolutionContext.tag) {
                JavadocTag.PARAM -> {
                    val name = tag.dataElements.firstOrNull()?.text.orEmpty()
                    val index =
                        (analysedElement as? PsiMethod)?.parameterList?.parameters?.map { it.name }?.indexOf(name)
                    Param(
                        wrapTagIfNecessary(
                            convertJavadocElements(
                                tag.contentElementsWithSiblingIfNeeded().drop(1),
                                context = resolutionContext.copy(name = name, parameterIndex = index)
                            )
                        ),
                        name
                    )
                }
                JavadocTag.THROWS, JavadocTag.EXCEPTION -> {
                    val resolved = tag.resolveToElement()
                    val dri = resolved?.let { DRI.from(it) }
                    val name = resolved?.getKotlinFqName()?.asString()
                        ?: tag.dataElements.firstOrNull()?.text.orEmpty()
                    Throws(
                        root = wrapTagIfNecessary(
                            convertJavadocElements(
                                tag.dataElements.drop(1),
                                context = resolutionContext.copy(name = name)
                            )
                        ),
                        /* we always would like to have a fully qualified name as name,
                        *  because it will be used as a display name later and we would like to have those unified
                        *  even if documentation states shortened version
                        *
                        *  Only if dri search fails we should use the provided phrase (since then we are not able to get a fq name)
                        * */
                        name = name,
                        exceptionAddress = dri
                    )
                }
                JavadocTag.RETURN -> Return(
                    wrapTagIfNecessary(
                        convertJavadocElements(
                            tag.contentElementsWithSiblingIfNeeded(),
                            context = resolutionContext
                        )
                    )
                )
                JavadocTag.AUTHOR -> Author(
                    wrapTagIfNecessary(
                        convertJavadocElements(
                            tag.contentElementsWithSiblingIfNeeded(),
                            context = resolutionContext
                        )
                    )
                ) // Workaround: PSI returns first word after @author tag as a `DOC_TAG_VALUE_ELEMENT`, then the rest as a `DOC_COMMENT_DATA`, so for `Name Surname` we get them parted
                JavadocTag.SEE -> {
                    val name =
                        tag.resolveToElement()?.getKotlinFqName()?.asString() ?: tag.referenceElement()?.text.orEmpty()
                    getSeeTagElementContent(tag, resolutionContext.copy(name = name)).let {
                        See(
                            wrapTagIfNecessary(it.first),
                            name,
                            it.second
                        )
                    }
                }
                JavadocTag.DEPRECATED -> Deprecated(
                    wrapTagIfNecessary(
                        convertJavadocElements(
                            tag.contentElementsWithSiblingIfNeeded(),
                            context = resolutionContext
                        )
                    )
                )
                else -> null
                //TODO https://github.com/Kotlin/dokka/issues/1618
            }
        } ?: CustomTagWrapper(
            wrapTagIfNecessary(
                convertJavadocElements(
                    tag.contentElementsWithSiblingIfNeeded(),
                    context = CommentResolutionContext(docComment, null)
                )
            ),
            tag.name
        )

    private fun wrapTagIfNecessary(list: List<DocTag>): CustomDocTag =
        if (list.size == 1 && (list.first() as? CustomDocTag)?.name == MarkdownElementTypes.MARKDOWN_FILE.name)
            list.first() as CustomDocTag
        else
            CustomDocTag(list, name = MarkdownElementTypes.MARKDOWN_FILE.name)

    private fun getSeeTagElementContent(
        tag: PsiDocTag,
        context: CommentResolutionContext
    ): Pair<List<DocTag>, DRI?> {
        val linkElement = tag.referenceElement()?.toDocumentationLink(context = context)
        val content = convertJavadocElements(
            tag.dataElements.dropWhile { it is PsiWhiteSpace || (it as? LazyParseablePsiElement)?.tokenType == JavaDocElementType.DOC_REFERENCE_HOLDER },
            context = context
        )
        return Pair(content, linkElement?.dri)
    }

    private fun PsiDocComment.getDescription(): Description? {
        return convertJavadocElements(
            descriptionElements.asIterable(),
            context = CommentResolutionContext(this, JavadocTag.DESCRIPTION)
        ).takeIf { it.isNotEmpty() }?.let {
            Description(wrapTagIfNecessary(it))
        }
    }

    private data class ParserState(
        val currentJavadocTag: JavadocTag?,
        val previousElement: PsiElement? = null,
        val openPreTags: Int = 0,
        val closedPreTags: Int = 0
    )

    private data class ParsingResult(val newState: ParserState, val parsedLine: String? = null) {
        constructor(tag: JavadocTag?) : this(ParserState(tag))

        operator fun plus(other: ParsingResult): ParsingResult =
            ParsingResult(
                other.newState,
                listOfNotNull(parsedLine, other.parsedLine).joinToString(separator = "")
            )
    }

    private inner class Parse : (Iterable<PsiElement>, Boolean, CommentResolutionContext) -> List<DocTag> {
        val driMap = mutableMapOf<String, DRI>()

        private fun PsiElement.stringify(state: ParserState, context: CommentResolutionContext): ParsingResult =
            when (this) {
                is PsiReference -> children.fold(ParsingResult(state)) { acc, e ->
                    acc + e.stringify(acc.newState, context)
                }
                else -> stringifySimpleElement(state, context)
            }

        private fun DocumentationContent.stringify(state: ParserState, context: CommentResolutionContext): ParsingResult =
            when(this){
                is PsiDocumentationContent -> psiElement.stringify(state, context)
                is DescriptorDocumentationContent -> {
                    val id = UUID.randomUUID()
                    inheritDocSections[id] = parseDocumentation(KotlinDocComment(element, descriptor), parseWithChildren = false)
                    ParsingResult(state, """<inheritdoc id="$id"/>""")
                }
                else -> throw IllegalStateException("Unrecognised documentation content: $this")
            }

        private fun PsiElement.stringifySimpleElement(
            state: ParserState,
            context: CommentResolutionContext
        ): ParsingResult {
            val openPre = state.openPreTags + "<pre(\\s+.*)?>".toRegex().findAll(text).toList().size
            val closedPre = state.closedPreTags + "</pre>".toRegex().findAll(text).toList().size
            val isInsidePre = openPre > closedPre
            val parsed = when (this) {
                is PsiInlineDocTag -> convertInlineDocTag(this, state.currentJavadocTag, context)
                is PsiDocParamRef -> toDocumentationLinkString()
                is PsiDocTagValue,
                is LeafPsiElement -> {
                    if (isInsidePre) {
                        /*
                        For values in the <pre> tag we try to keep formatting, so only the leading space is trimmed,
                        since it is there because it separates this line from the leading asterisk
                         */
                        text.let {
                            if ((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true && it.firstOrNull() == ' ')
                                it.drop(1) else it
                        }.let {
                            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true) it.dropLastWhile { it == ' ' } else it
                        }
                    } else {
                        /*
                        Outside of the <pre> we would like to trim everything from the start and end of a line since
                        javadoc doesn't care about it.
                         */
                        text.let {
                            if ((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true && text.isNotBlank() && state.previousElement !is PsiInlineDocTag) it?.trimStart() else it
                        }?.let {
                            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true && text.isNotBlank()) it.trimEnd() else it
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

            return (nextSibling as? PsiWhiteSpace)?.text?.startsWith("\n ") == true &&
                    (getNextSiblingIgnoringWhitespace() as? PsiDocToken)?.tokenType != JavaDocTokenTypes.INSTANCE.commentEnd() &&
                    nextNotEmptySibling?.isLeadingAsterisk() == true &&
                    furtherNotEmptySibling?.tokenType == JavaDocTokenTypes.INSTANCE.commentData() &&
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

        private fun convertInlineDocTag(
            tag: PsiInlineDocTag,
            javadocTag: JavadocTag?,
            context: CommentResolutionContext
        ) =
            when (tag.name) {
                "link", "linkplain" -> tag.referenceElement()
                    ?.toDocumentationLinkString(tag.dataElements.filterIsInstance<PsiDocToken>())
                "code", "literal" -> "<code data-inline>${tag.text}</code>"
                "index" -> "<index>${tag.children.filterIsInstance<PsiDocTagValue>().joinToString { it.text }}</index>"
                "inheritDoc" -> inheritDocResolver.resolveFromContext(context)
                    ?.fold(ParsingResult(javadocTag)) { result, e ->
                        result + e.stringify(result.newState, context)
                    }?.parsedLine.orEmpty()
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

        private fun createBlock(element: Element, insidePre: Boolean = false): List<DocTag> {
            val children = element.childNodes()
                .flatMap { convertHtmlNode(it, insidePre = insidePre || element.tagName() == "pre") }

            fun ifChildrenPresent(operation: () -> DocTag): List<DocTag> {
                return if (children.isNotEmpty()) listOf(operation()) else emptyList()
            }
            return when (element.tagName()) {
                "blockquote" -> ifChildrenPresent { BlockQuote(children) }
                "p" -> ifChildrenPresent { P(children) }
                "b" -> ifChildrenPresent { B(children) }
                "strong" -> ifChildrenPresent { Strong(children) }
                "index" -> listOf(Index(children))
                "i" -> ifChildrenPresent { I(children) }
                "em" -> listOf(Em(children))
                "code" -> ifChildrenPresent { if(insidePre) CodeBlock(children) else CodeInline(children) }
                "pre" -> if(children.size == 1 && children.first() is CodeInline) {
                    listOf(CodeBlock(children.first().children))
                } else {
                    listOf(Pre(children))
                }
                "ul" -> ifChildrenPresent { Ul(children) }
                "ol" -> ifChildrenPresent { Ol(children) }
                "li" -> listOf(Li(children))
                "a" -> listOf(createLink(element, children))
                "table" -> ifChildrenPresent { Table(children) }
                "tr" -> ifChildrenPresent { Tr(children) }
                "td" -> listOf(Td(children))
                "thead" -> listOf(THead(children))
                "tbody" -> listOf(TBody(children))
                "tfoot" -> listOf(TFoot(children))
                "caption" -> ifChildrenPresent { Caption(children) }
                "inheritdoc" -> {
                    val id = UUID.fromString(element.attr("id"))
                    val section = inheritDocSections[id]
                    val parsed = section?.children?.flatMap { it.root.children }.orEmpty()
                    if(parsed.size == 1 && parsed.first() is P){
                        parsed.first().children
                    } else {
                        parsed
                    }
                }
                else -> listOf(Text(body = element.ownText()))
            }
        }

        private fun convertHtmlNode(node: Node, insidePre: Boolean = false): List<DocTag> = when (node) {
            is TextNode -> (if (insidePre) {
                node.wholeText.takeIf { it.isNotBlank() }?.let { listOf(Text(body = it)) }
            } else {
                node.wholeText.parseHtmlEncodedWithNormalisedSpaces(renderWhiteCharactersAsSpaces = true)
            }).orEmpty()
            is Element -> createBlock(node)
            else -> emptyList()
        }

        override fun invoke(
            elements: Iterable<PsiElement>,
            asParagraph: Boolean,
            context: CommentResolutionContext
        ): List<DocTag> =
            elements.fold(ParsingResult(context.tag)) { acc, e ->
                acc + e.stringify(acc.newState, context)
            }.parsedLine?.let {
                val trimmed = it.trim()
                val toParse = if (asParagraph) "<p>$trimmed</p>" else trimmed
                Jsoup.parseBodyFragment(toParse).body().childNodes().flatMap { convertHtmlNode(it) }
            }.orEmpty()
    }

    private fun convertJavadocElements(
        elements: Iterable<PsiElement>,
        asParagraph: Boolean = true,
        context: CommentResolutionContext
    ): List<DocTag> =
        Parse()(elements, asParagraph, context)

    private fun PsiDocToken.isSharpToken() = tokenType == JavaDocTokenType.DOC_TAG_VALUE_SHARP_TOKEN

    private fun PsiDocToken.isLeadingAsterisk() = tokenType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS

    private fun PsiElement.toDocumentationLink(labelElement: PsiElement? = null, context: CommentResolutionContext) =
        resolveToGetDri()?.let {
            val dri = DRI.from(it)
            val label = labelElement ?: defaultLabel()
            DocumentationLink(dri, convertJavadocElements(listOfNotNull(label), asParagraph = false, context))
        }

    private fun PsiDocTag.referenceElement(): PsiElement? =
        linkElement()?.referenceElementOrSelf()

    private fun PsiElement.defaultLabel() = children.firstOrNull {
        it is PsiDocToken && it.text.isNotBlank() && !it.isSharpToken()
    } ?: this

    private fun PsiDocTag.linkElement(): PsiElement? =
        valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

    companion object {
        private const val UNRESOLVED_PSI_ELEMENT = "UNRESOLVED_PSI_ELEMENT"
    }
}
