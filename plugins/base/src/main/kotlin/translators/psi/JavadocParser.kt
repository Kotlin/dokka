package org.jetbrains.dokka.base.translators.psi

import com.intellij.psi.*
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Deprecated
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
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
                "param" -> Param(P(convertJavadocElements(tag.dataElements.toList())), tag.text)
                "throws" -> Throws(P(convertJavadocElements(tag.dataElements.toList())), tag.text)
                "return" -> Return(P(convertJavadocElements(tag.dataElements.toList())))
                "author" -> Author(P(convertJavadocElements(tag.dataElements.toList())))
                "see" -> See(P(getSeeTagElementContent(tag)), tag.referenceElement()?.text.orEmpty(), null)
                "deprecated" -> Deprecated(P(convertJavadocElements(tag.dataElements.toList())))
                else -> null
            }
        })
        return DocumentationNode(nodes)
    }

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

        return null
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

    private fun getSeeTagElementContent(tag: PsiDocTag): List<DocTag> =
        listOfNotNull(tag.referenceElement()?.toDocumentationLink())

    private fun PsiDocComment.getDescription(): Description? {
        val nonEmptyDescriptionElements = descriptionElements.filter { it.text.trim().isNotEmpty() }
        val convertedDescriptionElements = convertJavadocElements(nonEmptyDescriptionElements)
        if (convertedDescriptionElements.isNotEmpty()) {
            return Description(P(convertedDescriptionElements))
        }

        return null
    }

    private fun convertJavadocElements(elements: Iterable<PsiElement>): List<DocTag> =
        elements.mapNotNull {
            when (it) {
                is PsiReference -> convertJavadocElements(it.children.toList())
                is PsiInlineDocTag -> listOfNotNull(convertInlineDocTag(it))
                is PsiDocParamRef -> listOfNotNull(it.toDocumentationLink())
                is PsiDocTagValue,
                is LeafPsiElement -> Jsoup.parse(it.text.trim()).body().childNodes().mapNotNull(::convertHtmlNode)
                else -> null
            }
        }.flatten()

    private fun convertHtmlNode(node: Node, insidePre: Boolean = false): DocTag? = when (node) {
        is TextNode -> Text(body = if (insidePre) node.wholeText else node.text())
        is Element -> createBlock(node)
        else -> null
    }

    private fun createBlock(element: Element): DocTag {
        val children = element.childNodes().mapNotNull { convertHtmlNode(it) }
        return when (element.tagName()) {
            "p" -> P(listOf(Br, Br) + children)
            "b" -> B(children)
            "strong" -> Strong(children)
            "i" -> I(children)
            "em" -> Em(children)
            "code" -> CodeBlock(children)
            "pre" -> Pre(children)
            "ul" -> Ul(children)
            "ol" -> Ol(children)
            "li" -> Li(children)
            "a" -> createLink(element, children)
            else -> Text(body = element.ownText())
        }
    }

    private fun createLink(element: Element, children: List<DocTag>): DocTag {
        return when {
            element.hasAttr("docref") -> {
                A(children, params = mapOf("docref" to element.attr("docref")))
            }
            element.hasAttr("href") -> {
                A(children, params = mapOf("href" to element.attr("href")))
            }
            else -> Text(children = children)
        }
    }

    private fun PsiDocToken.isSharpToken() = tokenType.toString() == "DOC_TAG_VALUE_SHARP_TOKEN"

    private fun PsiElement.toDocumentationLink(labelElement: PsiElement? = null) =
        reference?.resolve()?.let {
            val dri = DRI.from(it)
            val label = labelElement ?: children.firstOrNull {
                it is PsiDocToken && it.text.isNotBlank() && !it.isSharpToken()
            } ?: this
            DocumentationLink(dri, convertJavadocElements(listOfNotNull(label)))
        }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when (tag.name) {
        "link", "linkplain" -> {
            tag.referenceElement()?.toDocumentationLink(tag.dataElements.firstIsInstanceOrNull<PsiDocToken>())
        }
        "code", "literal" -> {
            CodeInline(listOf(Text(tag.text)))
        }
        "index" -> Index(tag.children.filterIsInstance<PsiDocTagValue>().map { Text(it.text) })
        else -> Text(tag.text)
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
}
