package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.analysis.kotlin.internal.SampleProvider
import org.jetbrains.dokka.analysis.kotlin.internal.SampleProviderFactory
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile

class KotlinSampleProviderFactory(val context: DokkaContext): SampleProviderFactory {
    override fun build(): SampleProvider {
        return KotlinSampleProvider(context)
    }

}
/**
 * It's declared as open since StdLib has its own sample transformer
 * with [processBody] and [processImports]
 */
@InternalDokkaApi
open class KotlinSampleProvider(val context: DokkaContext): SampleProvider {
    private val kotlinAnalysis = SamplesKotlinAnalysis(
        sourceSets = context.configuration.sourceSets,
        context = context,
        projectKotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    )

    protected open fun processBody(psiElement: PsiElement): String {
        val text = processSampleBody(psiElement).trim { it == '\n' || it == '\r' }.trimEnd()
        val lines = text.split("\n")
        val indent = lines.filter(String::isNotBlank).minOfOrNull { it.takeWhile(Char::isWhitespace).count() } ?: 0
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

    protected open fun processImports(psiElement: PsiElement): String {
        val psiFile = psiElement.containingFile
        return when(val text = (psiFile as? KtFile)?.importList?.text) {
            is String -> text
            else -> ""
        }
    }

    /**
     * @return [SampleProvider.SampleSnippet] or null if it has not found by [fqLink]
     */
    override fun getSample(sourceSet: DokkaConfiguration.DokkaSourceSet, fqLink: String): SampleProvider.SampleSnippet? {
        val analysisContext = kotlinAnalysis[sourceSet]
        val psiElement = analyze(analysisContext.mainModule) {
            val lastDotIndex = fqLink.lastIndexOf('.')

            val functionName = if (lastDotIndex == -1) fqLink else fqLink.substring(lastDotIndex + 1, fqLink.length)
            val packageName = if (lastDotIndex == -1) "" else fqLink.substring(0, lastDotIndex)
            getTopLevelCallableSymbols(FqName(packageName), Name.identifier(functionName)).firstOrNull()?.psi
        }
            ?: return null.also { context.logger.warn("Cannot find PsiElement corresponding to $fqLink") }
        val imports =
            processImports(psiElement)
        val body = processBody(psiElement)

        return SampleProvider.SampleSnippet(imports, body)
    }
    override fun close() {
        kotlinAnalysis.close()
    }
}