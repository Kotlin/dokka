package org.jetbrains.dokka.base.transformers.pages.samples

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.idea.kdoc.resolveKDocSampleLink
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DefaultSamplesTransformer(context: DokkaContext) : SamplesTransformer(context) {

    override fun processBody(psiElement: PsiElement): String {
        val text = processSampleBody(psiElement).trim { it == '\n' || it == '\r' }.trimEnd()
        val lines = text.split("\n")
        val indent = lines.filter(String::isNotBlank).map { it.takeWhile(Char::isWhitespace).count() }.minOrNull() ?: 0
        return lines.joinToString("\n") { it.drop(indent) }
    }

    private fun processSampleBody(psiElement: PsiElement): String = when (psiElement) {
        is KtDeclarationWithBody -> {
            val bodyExpression = psiElement.bodyExpression
            when (bodyExpression) {
                is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                else -> bodyExpression!!.text
            }
        }
        else -> psiElement.text
    }

    override fun processImports(psiElement: PsiElement): String {
        val psiFile = psiElement.containingFile
        return when(val text = psiFile.safeAs<KtFile>()?.importList?.text) {
            is String -> text
            else -> ""
        }
    }
}