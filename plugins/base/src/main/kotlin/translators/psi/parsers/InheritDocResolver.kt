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
    val tag: JavadocTag,
    val name: String? = null,
    val parameterIndex: Int? = null,
)

internal class InheritDocResolver(
    private val logger: DokkaLogger
) {
    internal fun resolveFromContext(context: CommentResolutionContext) =
        when (context.tag) {
            JavadocTag.THROWS -> context.name?.let { name -> resolveThrowsTag(context.comment, name) }
            JavadocTag.PARAM -> context.parameterIndex?.let { paramIndex -> resolveParamTag(context.comment, paramIndex) }
            JavadocTag.DEPRECATED -> resolveGenericTag(context.comment, JavadocTag.DESCRIPTION)
            JavadocTag.SEE -> emptyList()
            else -> resolveGenericTag(context.comment, context.tag)
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

    private fun resolveThrowsTag(
        currentElement: PsiDocComment,
        exceptionFqName: String
    ): List<PsiElement> =
        (currentElement.owner as? PsiMethod)?.let { method -> lowestMethodsWithTag(method, JavadocTag.THROWS) }
            .orEmpty().firstOrNull {
                findClosestDocComment(it, logger)?.hasThrowsTagWithExceptionOfType(exceptionFqName) == true
            }?.docComment?.tagsByName(JavadocTag.THROWS)?.flatMap {
                when (it) {
                    is PsiDocTag -> it.contentElementsWithSiblingIfNeeded()
                    else -> listOf(it)
                }
            }?.drop(1).orEmpty()

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
            }.drop(1)

    //if we are in psi class javadoc only inherits docs from classes and not from interfaces
    private fun lowestClassWithTag(baseClass: PsiClass, javadocTag: JavadocTag): PsiDocComment? =
        baseClass.superClass?.let {
            val tag = findClosestDocComment(it, logger)
            return if (tag?.hasTag(javadocTag) == true) tag
            else lowestClassWithTag(it, javadocTag)
        }

    private fun lowestMethodWithTag(
        baseMethod: PsiMethod,
        javadocTag: JavadocTag,
    ): PsiDocComment? =
        lowestMethodsWithTag(baseMethod, javadocTag).firstOrNull()?.docComment

    private fun lowestMethodsWithTag(baseMethod: PsiMethod, javadocTag: JavadocTag) =
        baseMethod.findSuperMethods().filter { findClosestDocComment(it, logger)?.hasTag(javadocTag) == true }

    private fun PsiDocComment.hasThrowsTagWithExceptionOfType(exceptionFqName: String): Boolean =
        hasTag(JavadocTag.THROWS) && tagsByName(JavadocTag.THROWS).firstIsInstanceOrNull<PsiDocTag>()
            ?.resolveToElement()
            ?.getKotlinFqName()?.asString() == exceptionFqName
}