package org.jetbrains.dokka.Samples

import com.google.inject.Inject
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.resolve.ImportPath

open class KotlinWebsiteSampleProcessingService
@Inject constructor(options: DocumentationOptions,
                    logger: DokkaLogger,
                    resolutionFacade: DokkaResolutionFacade)
    : DefaultSampleProcessingService(options, logger, resolutionFacade) {

    private class SampleBuilder : KtTreeVisitorVoid() {
        val builder = StringBuilder()
        val text: String
            get() = builder.toString()

        fun KtValueArgument.extractStringArgumentValue() =
                (getArgumentExpression() as KtStringTemplateExpression)
                        .entries.joinToString("") { it.text }


        fun convertAssertPrints(expression: KtCallExpression) {
            val (argument, commentArgument) = expression.valueArguments
            builder.apply {
                append("println(")
                append(argument.text)
                append(") // ")
                append(commentArgument.extractStringArgumentValue())
            }
        }

        fun convertAssertTrueFalse(expression: KtCallExpression, expectedResult: Boolean) {
            val (argument) = expression.valueArguments
            builder.apply {
                expression.valueArguments.getOrNull(1)?.let {
                    append("// ${it.extractStringArgumentValue()}")
                    val ws = expression.prevLeaf { it is PsiWhiteSpace }
                    append(ws?.text ?: "\n")
                }
                append("println(\"")
                append(argument.text)
                append(" is \${")
                append(argument.text)
                append("}\") // $expectedResult")
            }
        }

        fun convertAssertFails(expression: KtCallExpression) {
            val (message, funcArgument) = expression.valueArguments
            builder.apply {
                val argument = if (funcArgument.getArgumentExpression() is KtLambdaExpression)
                    PsiTreeUtil.findChildOfType(funcArgument, KtBlockExpression::class.java)?.text ?: ""
                else
                    funcArgument.text
                append(argument.lines().joinToString(separator = "\n") { "// $it" })
                append(" // ")
                append(message.extractStringArgumentValue())
                append(" will fail")
            }
        }

        fun convertAssertFailsWith(expression: KtCallExpression) {
            val (funcArgument) = expression.valueArguments
            val (exceptionType) = expression.typeArguments
            builder.apply {
                val argument = if (funcArgument.firstChild is KtLambdaExpression)
                    PsiTreeUtil.findChildOfType(funcArgument, KtBlockExpression::class.java)?.text ?: ""
                else
                    funcArgument.text
                append(argument.lines().joinToString(separator = "\n") { "// $it" })
                append(" // will fail with ")
                append(exceptionType.text)
            }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            when (expression.calleeExpression?.text) {
                "assertPrints" -> convertAssertPrints(expression)
                "assertTrue" -> convertAssertTrueFalse(expression, expectedResult = true)
                "assertFalse" -> convertAssertTrueFalse(expression, expectedResult = false)
                "assertFails" -> convertAssertFails(expression)
                "assertFailsWith" -> convertAssertFailsWith(expression)
                else -> super.visitCallExpression(expression)
            }
        }

        override fun visitElement(element: PsiElement) {
            if (element is LeafPsiElement)
                builder.append(element.text)
            super.visitElement(element)
        }
    }

    private fun PsiElement.buildSampleText(): String {
        val sampleBuilder = SampleBuilder()
        this.accept(sampleBuilder)
        return sampleBuilder.text
    }

    val importsToIgnore = arrayOf("samples.*").map { ImportPath.fromString(it) }

    override fun processImports(psiElement: PsiElement): ContentBlockCode {
        val psiFile = psiElement.containingFile
        if (psiFile is KtFile) {
            return ContentBlockCode("kotlin").apply {
                append(ContentText("\n"))
                psiFile.importList?.let {
                    it.allChildren.filter {
                        it !is KtImportDirective || it.importPath !in importsToIgnore
                    }.forEach { append(ContentText(it.text)) }
                }
            }
        }
        return super.processImports(psiElement)
    }

    override fun processSampleBody(psiElement: PsiElement) = when (psiElement) {
        is KtDeclarationWithBody -> {
            val bodyExpression = psiElement.bodyExpression
            val bodyExpressionText = bodyExpression!!.buildSampleText()
            when (bodyExpression) {
                is KtBlockExpression -> bodyExpressionText.removeSurrounding("{", "}")
                else -> bodyExpressionText
            }
        }
        else -> psiElement.buildSampleText()
    }
}

