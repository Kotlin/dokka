/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl

import com.intellij.psi.PsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KDocFinder
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.SamplesKotlinAnalysis
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.analysis.kotlin.internal.SampleProvider
import org.jetbrains.dokka.analysis.kotlin.internal.SampleProviderFactory
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

public class KotlinSampleProviderFactory(
    public val context: DokkaContext
): SampleProviderFactory {
    override fun build(): SampleProvider {
        return KotlinSampleProvider(context)
    }

}
/**
 * It's declared as open since StdLib has its own sample transformer
 * with [processBody] and [processImports]
 */
@InternalDokkaApi
public open class KotlinSampleProvider(
    public val context: DokkaContext
): SampleProvider {
    private val kDocFinder: KDocFinder = context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { kdocFinder }
    private val analysis = lazy {
        /**
         * Run from the thread of [Dispatchers.Default]. It can help to avoid a memory leaks in `ThreadLocal`s (that keep `URLCLassLoader`)
         * since we shut down Dispatchers.Default at the end of each task (see [org.jetbrains.dokka.DokkaConfiguration.finalizeCoroutines]).
         * Currently, all `ThreadLocal`s are in a compiler/IDE codebase.
         */
        runBlocking(Dispatchers.Default) {
            @OptIn(DokkaPluginApiPreview::class)
            SamplesKotlinAnalysis(
                sourceSets = context.configuration.sourceSets,
                context = context,
                projectKotlinAnalysis = context.plugin<CompilerDescriptorAnalysisPlugin>()
                    .querySingle { kotlinAnalysis }
            )
        }
    }
    protected open fun processBody(psiElement: PsiElement): String {
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
        return runBlocking(Dispatchers.Default) {
                val resolveSession = analysis.value[sourceSet].resolveSession
                val psiElement = fqNameToPsiElement(resolveSession, fqLink, sourceSet)
                    ?: return@runBlocking null.also { context.logger.warn("Cannot find PsiElement corresponding to $fqLink") }
                val imports =
                    processImports(psiElement)
                val body = processBody(psiElement)
                return@runBlocking SampleProvider.SampleSnippet(imports, body)
        }
    }
    override fun close() {
        if(analysis.isInitialized())
            analysis.value.close()
    }

    private fun fqNameToPsiElement(resolveSession: ResolveSession, functionName: String, dokkaSourceSet: DokkaConfiguration.DokkaSourceSet): PsiElement? {
        val packageName = functionName.takeWhile { it != '.' }
        val descriptor = resolveSession.getPackageFragment(FqName(packageName))
            ?: return null.also { context.logger.warn("Cannot find descriptor for package $packageName") }

        with (kDocFinder) {
            val symbol = resolveKDocLink(
                descriptor,
                functionName,
                dokkaSourceSet,
                emptyBindingContext = true
            ).firstOrNull() ?: return null.also { context.logger.warn("Unresolved function $functionName in @sample") }
            return org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration(symbol)
        }
    }
}
