package org.jetbrains.dokka.Samples

import com.google.inject.Inject
import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DocumentationOptions
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.DokkaResolutionFacade
import org.jetbrains.kotlin.psi.*

open class KotlinWebsiteSampleProcessingService
@Inject constructor(options: DocumentationOptions,
                    logger: DokkaLogger,
                    resolutionFacade: DokkaResolutionFacade)
    : DefaultSampleProcessingService(options, logger, resolutionFacade) {

    private class SampleBuilder() : KtVisitorVoid() {
        val builder = StringBuilder()
        val text: String
            get() = builder.toString()

        fun convertAssertPrints(expression: KtCallExpression) {
            val (argument, commentArgument) = expression.valueArguments
            val comment = commentArgument.getArgumentExpression() as KtStringTemplateExpression
            val commentText = comment.entries.joinToString("") { it.text }
            builder.apply {
                append("println(")
                append(argument.text)
                append(") // ")
                append(commentText)
            }
        }

        fun convertAssertTrue(expression: KtCallExpression) {
            val (argument) = expression.valueArguments
            builder.apply {
                append("println(\"")
                append(argument.text)
                append(" is \${")
                append(argument.text)
                append("}\") // true")
            }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            when (expression.calleeExpression?.text) {
                "assertPrints" -> convertAssertPrints(expression)
                "assertTrue" -> convertAssertTrue(expression)
                else -> super.visitCallExpression(expression)
            }
        }

        override fun visitElement(element: PsiElement?) {
            if (element != null) {
                if (element.children.isEmpty())
                    builder.append(element.text)
                else
                    element.acceptChildren(this)
            }
        }
    }

    private fun PsiElement.buildSampleText(): String {
        val sampleBuilder = SampleBuilder()
        this.accept(sampleBuilder)
        return sampleBuilder.text
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

