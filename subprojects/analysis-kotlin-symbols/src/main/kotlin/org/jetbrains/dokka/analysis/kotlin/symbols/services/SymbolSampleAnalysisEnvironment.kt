/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

internal class SymbolSampleAnalysisEnvironmentCreator(
    private val context: DokkaContext,
) : SampleAnalysisEnvironmentCreator {

    private val symbolAnalysisPlugin = context.plugin<SymbolsAnalysisPlugin>()

    override fun <T> use(block: SampleAnalysisEnvironment.() -> T): T {
        return runBlocking(Dispatchers.Default) {
            SamplesKotlinAnalysis(
                sourceSets = context.configuration.sourceSets,
                context = context,
                projectKotlinAnalysis = symbolAnalysisPlugin.querySingle { kotlinAnalysis }
            ).use { kotlinAnalysis ->
                val sampleAnalysis = SymbolSampleAnalysisEnvironment(
                    kotlinAnalysis = kotlinAnalysis,
                    dokkaLogger = context.logger
                )
                block(sampleAnalysis)
            }
        }
    }
}

private class SymbolSampleAnalysisEnvironment(
    private val kotlinAnalysis: KotlinAnalysis,
    private val dokkaLogger: DokkaLogger,
) : SampleAnalysisEnvironment {

    override fun resolveSample(sourceSet: DokkaSourceSet, fullyQualifiedLink: String): SampleSnippet? {
        val psiElement = findPsiElement(sourceSet, fullyQualifiedLink)
        if (psiElement == null) {
            dokkaLogger.warn(
                "Unable to resolve a @sample link: \"$fullyQualifiedLink\". Is it used correctly? " +
                        "Expecting a link to a reachable (resolvable) top-level Kotlin function."
            )
            return null
        } else if (psiElement.containingFile !is KtFile) {
            dokkaLogger.warn("Unable to resolve non-Kotlin @sample links: \"$fullyQualifiedLink\"")
            return null
        } else if (psiElement !is KtFunction) {
            dokkaLogger.warn("Unable to process a @sample link: \"$fullyQualifiedLink\". Only function links allowed.")
            return null
        }

        val imports = processImports(psiElement)
        val body = processBody(psiElement)

        return SampleSnippet(imports, body)
    }

    private fun findPsiElement(sourceSet: DokkaSourceSet, fqLink: String): PsiElement? {
        val analysisContext = kotlinAnalysis[sourceSet]
        return analyze(analysisContext.mainModule) {
            // TODO the logic below is incorrect as it assumes the samples can only link to top-level functions.
            // TODO should be corrected to be able to work with functions inside classes. See Descriptor's impl.
            val isRootPackage = !fqLink.contains('.')
            val supposedFunctionName = if (isRootPackage) fqLink else fqLink.substringAfterLast(".")
            val supposedPackageName = if (isRootPackage) "" else fqLink.substringBeforeLast(".")

            getTopLevelCallableSymbols(FqName(supposedPackageName), Name.identifier(supposedFunctionName)).firstOrNull()?.psi
        }
    }

    private fun processImports(psiElement: PsiElement): List<String> {
        val psiFile = psiElement.containingFile
        val importsList = (psiFile as? KtFile)?.importList ?: return emptyList()
        return importsList.imports
            .map { it.text.removePrefix("import ") }
            .filter { it.isNotBlank() }
    }

    private fun processBody(sampleElement: PsiElement): String {
        return getSampleBody(sampleElement)
            .trim { it == '\n' || it == '\r' }
            .trimEnd()
            .trimIndent()
    }

    private fun getSampleBody(sampleElement: PsiElement): String {
        return when (sampleElement) {
            is KtDeclarationWithBody -> {
                when (val bodyExpression = sampleElement.bodyExpression) {
                    is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                    else -> bodyExpression!!.text
                }
            }

            else -> sampleElement.text
        }
    }
}
