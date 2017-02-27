package org.jetbrains.dokka.Samples

import com.google.inject.Inject
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
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

    val importsToIgnore = arrayOf("samples.*").map(::ImportPath)

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

