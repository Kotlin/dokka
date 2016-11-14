package Samples

import com.google.inject.Inject
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DocumentationOptions
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.DokkaResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.util.*

open class KotlinWebsiteSampleProcessingService
@Inject constructor(options: DocumentationOptions,
                    logger: DokkaLogger,
                    resolutionFacade: DokkaResolutionFacade)
    : DefaultSampleProcessingService(options, logger, resolutionFacade) {


    fun buildReplacementData(psiElement: PsiElement): Map<TextRange, String> {

        val result = TreeMap<TextRange, String>({ o1, o2 -> o1.startOffset.compareTo(o2.startOffset) })
        fun convertAssertPrints(expression: KtCallExpression) {
            val (argument, commentArgument) = expression.valueArguments
            val comment = commentArgument.getArgumentExpression() as KtStringTemplateExpression
            val commentText = comment.entries.joinToString("") { it.text }
            result[expression.textRange] = "println(${argument.text}) //$commentText"
        }

        fun convertAssertTrue(expression: KtCallExpression) {
            val (argument) = expression.valueArguments
            result[expression.textRange] = "println(\"${argument.text} is \${${argument.text}}\") //true "
        }

        if (psiElement is KtElement) {
            val visitor = object : KtTreeVisitor<Any>() {
                override fun visitCallExpression(expression: KtCallExpression, data: Any?): Void? {
                    when (expression.calleeExpression?.text) {
                        "assertPrints" -> convertAssertPrints(expression)
                        "assertTrue" -> convertAssertTrue(expression)
                        else -> super.visitCallExpression(expression, data)
                    }
                    return null
                }
            }
            psiElement.acceptChildren(visitor)
        }
        return result
    }

    private fun String.applyReplacements(baseOffset: Int, replacementData: Map<TextRange, String>): String {
        val partsList = arrayListOf<String>()
        var prevRange = TextRange(0, baseOffset)
        for ((range, replacement) in replacementData) {
            partsList.add(substring(prevRange.endOffset - baseOffset, range.startOffset - baseOffset))
            partsList.add(replacement)
            prevRange = range
        }
        partsList.add(substring(prevRange.endOffset - baseOffset))
        return partsList.joinToString(separator = "")
    }


    override fun processSampleBody(psiElement: PsiElement): String {

        val replacementData = buildReplacementData(psiElement)

        return when (psiElement) {
            is KtDeclarationWithBody -> {
                val bodyExpression = psiElement.bodyExpression
                val bodyExpressionText = bodyExpression!!.text.applyReplacements(bodyExpression.startOffset, replacementData)
                when (bodyExpression) {
                    is KtBlockExpression -> bodyExpressionText.removeSurrounding("{", "}")
                    else -> bodyExpressionText
                }
            }
            else -> psiElement.text.applyReplacements(psiElement.startOffset, replacementData)
        }
    }
}

