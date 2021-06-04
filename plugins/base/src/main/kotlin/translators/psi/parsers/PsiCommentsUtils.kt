package org.jetbrains.dokka.base.translators.psi.parsers

import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.translators.psi.findSuperMethodsOrEmptyArray
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal interface DocComment {
    fun hasTag(tag: JavadocTag): Boolean
    fun hasTagWithExceptionOfType(tag: JavadocTag, exceptionFqName: String): Boolean
    fun tagsByName(tag: JavadocTag, param: String? = null): List<DocumentationContent>
}

internal data class JavaDocComment(val comment: PsiDocComment) : DocComment {
    override fun hasTag(tag: JavadocTag): Boolean = comment.hasTag(tag)
    override fun hasTagWithExceptionOfType(tag: JavadocTag, exceptionFqName: String): Boolean =
        comment.hasTag(tag) && comment.tagsByName(tag).firstIsInstanceOrNull<PsiDocTag>()
            ?.resolveToElement()
            ?.getKotlinFqName()?.asString() == exceptionFqName

    override fun tagsByName(tag: JavadocTag, param: String?): List<DocumentationContent> =
        comment.tagsByName(tag).map { PsiDocumentationContent(it, tag) }
}

internal data class KotlinDocComment(val comment: KDocTag, val descriptor: DeclarationDescriptor) : DocComment {
    override fun hasTag(tag: JavadocTag): Boolean =
        when (tag) {
            JavadocTag.DESCRIPTION -> comment.getContent().isNotEmpty()
            else -> tagsWithContent.any { it.text.startsWith("@$tag") }
        }

    override fun hasTagWithExceptionOfType(tag: JavadocTag, exceptionFqName: String): Boolean =
        tagsWithContent.any { it.hasExceptionWithName(tag, exceptionFqName) }

    override fun tagsByName(tag: JavadocTag, param: String?): List<DocumentationContent> =
        when (tag) {
            JavadocTag.DESCRIPTION -> listOf(DescriptorDocumentationContent(descriptor, comment, tag))
            else -> comment.children.mapNotNull { (it as? KDocTag) }
                .filter { it.name == "$tag" && param?.let { param -> it.hasExceptionWithName(param) } != false }
                .map { DescriptorDocumentationContent(descriptor, it, tag) }
        }

    private val tagsWithContent: List<KDocTag> = comment.children.mapNotNull { (it as? KDocTag) }

    private fun KDocTag.hasExceptionWithName(tag: JavadocTag, exceptionFqName: String) =
        text.startsWith("@$tag") && hasExceptionWithName(exceptionFqName)

    private fun KDocTag.hasExceptionWithName(exceptionFqName: String) =
        getSubjectName() == exceptionFqName
}

internal interface DocumentationContent {
    val tag: JavadocTag
}

internal data class PsiDocumentationContent(val psiElement: PsiElement, override val tag: JavadocTag) :
    DocumentationContent

internal data class DescriptorDocumentationContent(
    val descriptor: DeclarationDescriptor,
    val element: KDocTag,
    override val tag: JavadocTag
) : DocumentationContent

internal fun PsiDocComment.hasTag(tag: JavadocTag): Boolean =
    when (tag) {
        JavadocTag.DESCRIPTION -> descriptionElements.isNotEmpty()
        else -> findTagByName(tag.toString()) != null
    }

internal fun PsiDocComment.tagsByName(tag: JavadocTag): List<PsiElement> =
    when (tag) {
        JavadocTag.DESCRIPTION -> descriptionElements.toList()
        else -> findTagsByName(tag.toString()).toList()
    }

internal fun findClosestDocComment(element: PsiNamedElement, logger: DokkaLogger): DocComment? {
    (element as? PsiDocCommentOwner)?.docComment?.run { return JavaDocComment(this) }
    element.toKdocComment()?.run { return this }

    if (element is PsiMethod) {
        val superMethods = element.findSuperMethodsOrEmptyArray(logger)
        if (superMethods.isEmpty()) return null

        if (superMethods.size == 1) {
            return findClosestDocComment(superMethods.single(), logger)
        }

        val superMethodDocumentation = superMethods.map { method -> findClosestDocComment(method, logger) }
        if (superMethodDocumentation.size == 1) {
            return superMethodDocumentation.single()
        }

        logger.debug(
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
    return element.children.firstIsInstanceOrNull<PsiDocComment>()?.let { JavaDocComment(it) }
}

internal fun PsiNamedElement.toKdocComment(): KotlinDocComment? =
    (navigationElement as? KtElement)?.findKDoc { DescriptorToSourceUtils.descriptorToDeclaration(it) }
        ?.run {
            (this@toKdocComment.navigationElement as? KtDeclaration)?.descriptor?.let {
                KotlinDocComment(
                    this,
                    it
                )
            }
        }

internal fun PsiDocTag.contentElementsWithSiblingIfNeeded(): List<PsiElement> = if (dataElements.isNotEmpty()) {
    listOfNotNull(
        dataElements[0],
        dataElements[0].nextSibling?.takeIf { it.text != dataElements.drop(1).firstOrNull()?.text },
        *dataElements.drop(1).toTypedArray()
    )
} else {
    emptyList()
}

internal fun PsiDocTag.resolveToElement(): PsiElement? =
    dataElements.firstOrNull()?.firstChild?.referenceElementOrSelf()?.resolveToGetDri()