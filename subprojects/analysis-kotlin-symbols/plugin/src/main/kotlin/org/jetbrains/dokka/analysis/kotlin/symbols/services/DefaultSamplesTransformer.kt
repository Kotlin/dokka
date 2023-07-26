package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile

internal class DefaultSamplesTransformer(context: DokkaContext) : SamplesTransformerImpl(context) {

    override fun processBody(psiElement: PsiElement): String {
        val text = processSampleBody(psiElement).trim { it == '\n' || it == '\r' }.trimEnd()
        val lines = text.split("\n")
        val indent = lines.filter(String::isNotBlank).map { it.takeWhile(Char::isWhitespace).count() }.minOrNull() ?: 0
        return lines.joinToString("\n") { it.drop(indent) }
    }

    private fun processSampleBody(psiElement: PsiElement): String = when (psiElement) {
        is KtDeclarationWithBody -> {
            when (val bodyExpression = psiElement.bodyExpression) {
                is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                else -> bodyExpression!!.text
            }
        }
        else -> psiElement.text
    }

    override fun processImports(psiElement: PsiElement): String {
        val psiFile = psiElement.containingFile
        return when(val text = (psiFile as? KtFile)?.importList?.text) {
            is String -> text
            else -> ""
        }
    }
}
