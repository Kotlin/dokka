package org.jetbrains.dokka.base.translators.psi.parsers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal data class CommentResolutionContext(
    val comment: PsiDocComment,
    val tag: JavadocTag?,
    val name: String? = null,
    val parameterIndex: Int? = null,
)

internal class InheritDocResolver(
    private val logger: DokkaLogger
) {
    internal fun resolveFromContext(context: CommentResolutionContext) =
        when (context.tag) {
            JavadocTag.THROWS, JavadocTag.EXCEPTION -> context.name?.let { name -> resolveThrowsTag(context.tag, context.comment, name) }
            JavadocTag.PARAM -> context.parameterIndex?.let { paramIndex -> resolveParamTag(context.comment, paramIndex) }
            JavadocTag.DEPRECATED -> resolveGenericTag(context.comment, JavadocTag.DESCRIPTION)
            JavadocTag.SEE -> emptyList()
            else -> context.tag?.let { tag -> resolveGenericTag(context.comment, tag) }
        }

    private fun resolveGenericTag(currentElement: PsiDocComment, tag: JavadocTag): List<PsiElement> =
        when (val owner = currentElement.owner) {
            is PsiClass -> lowestClassWithTag(owner, tag)
            is PsiMethod -> lowestMethodWithTag(owner, tag)
            else -> null
        }?.tagsByName(tag)?.flatMap {
            when (it) {
                is PsiDocTag -> it.contentElementsWithSiblingIfNeeded()
                else -> listOf(it)
            }
        }.orEmpty()

    /**
     * Main resolution point for exception like tags
     *
     * This should be used only with [JavadocTag.EXCEPTION] or [JavadocTag.THROWS] as their resolution path should be the same
     */
    private fun resolveThrowsTag(
        tag: JavadocTag,
        currentElement: PsiDocComment,
        exceptionFqName: String
    ): List<PsiElement> =
        (currentElement.owner as? PsiMethod)?.let { method -> lowestMethodsWithTag(method, tag) }
            .orEmpty().firstOrNull {
                findClosestDocComment(it, logger)?.hasTagWithExceptionOfType(tag, exceptionFqName) == true
            }?.docComment?.tagsByName(tag)?.flatMap {
                when (it) {
                    is PsiDocTag -> it.contentElementsWithSiblingIfNeeded()
                    else -> listOf(it)
                }
            }?.withoutReferenceLink().orEmpty()

    private fun resolveParamTag(
        currentElement: PsiDocComment,
        parameterIndex: Int,
    ): List<PsiElement> =
        (currentElement.owner as? PsiMethod)?.let { method -> lowestMethodsWithTag(method, JavadocTag.PARAM) }
            .orEmpty().flatMap {
                if (parameterIndex >= it.parameterList.parametersCount || parameterIndex < 0) emptyList()
                else {
                    val closestTag = findClosestDocComment(it, logger)
                    val hasTag = closestTag?.hasTag(JavadocTag.PARAM)
                    if (hasTag != true) emptyList()
                    else {
                        val parameterName = it.parameterList.parameters[parameterIndex].name
                        closestTag.tagsByName(JavadocTag.PARAM)
                            .filterIsInstance<PsiDocTag>().map { it.contentElementsWithSiblingIfNeeded() }.firstOrNull {
                                it.firstOrNull()?.text == parameterName
                            }.orEmpty()
                    }
                }
            }.withoutReferenceLink()

    //if we are in psi class javadoc only inherits docs from classes and not from interfaces
    private fun lowestClassWithTag(baseClass: PsiClass, javadocTag: JavadocTag): PsiDocComment? =
        baseClass.superClass?.let {
            findClosestDocComment(it, logger)?.takeIf { tag -> tag.hasTag(javadocTag) } ?:
            lowestClassWithTag(it, javadocTag)
        }

    private fun lowestMethodWithTag(
        baseMethod: PsiMethod,
        javadocTag: JavadocTag,
    ): PsiDocComment? =
        lowestMethodsWithTag(baseMethod, javadocTag).firstOrNull()?.docComment

    private fun lowestMethodsWithTag(baseMethod: PsiMethod, javadocTag: JavadocTag) =
        baseMethod.findSuperMethods().filter { findClosestDocComment(it, logger)?.hasTag(javadocTag) == true }

    private fun PsiDocComment.hasTagWithExceptionOfType(tag: JavadocTag, exceptionFqName: String): Boolean =
        hasTag(tag) && tagsByName(tag).firstIsInstanceOrNull<PsiDocTag>()
            ?.resolveToElement()
            ?.getKotlinFqName()?.asString() == exceptionFqName

    private fun List<PsiElement>.withoutReferenceLink(): List<PsiElement> = drop(1)
}