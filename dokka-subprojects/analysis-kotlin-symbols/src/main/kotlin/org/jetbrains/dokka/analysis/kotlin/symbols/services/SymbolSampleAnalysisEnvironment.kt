/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DelicateDokkaApi
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleRewriter
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocTextLinkToSymbol
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromFunction
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromVariable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal class SymbolSampleAnalysisEnvironmentCreator(
    private val context: DokkaContext,
) : SampleAnalysisEnvironmentCreator {

    private val projectKotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val sampleRewriter by lazy {
        val rewriters = context.plugin<KotlinAnalysisPlugin>().query { sampleRewriter }
        if (rewriters.size > 1) context.logger.warn("There are more than one samples rewriters. Dokka does not support it.")
        rewriters.singleOrNull()
    }

    override fun <T> use(block: SampleAnalysisEnvironment.() -> T): T {
        return runBlocking(Dispatchers.Default) {
            create().use(block)
        }
    }

    @OptIn(DelicateDokkaApi::class)
    override fun create(): SampleAnalysisEnvironment {
        return SymbolSampleAnalysisEnvironment(
            samplesKotlinAnalysis = SamplesKotlinAnalysis(
                sourceSets = context.configuration.sourceSets,
                context = context
            ),
            projectKotlinAnalysis = projectKotlinAnalysis,
            sampleRewriter = sampleRewriter,
            dokkaLogger = context.logger
        )
    }
}

private class SymbolSampleAnalysisEnvironment(
    private val samplesKotlinAnalysis: KotlinAnalysis,
    private val projectKotlinAnalysis: KotlinAnalysis,
    private val sampleRewriter: SampleRewriter?,
    private val dokkaLogger: DokkaLogger,
) : SampleAnalysisEnvironment {

    override fun resolveSample(
        sourceSet: DokkaSourceSet,
        fullyQualifiedLink: String
    ): SampleSnippet? {
        val psiElement = findPsiElement(sourceSet, fullyQualifiedLink)
        if (psiElement == null) {
            dokkaLogger.warn(
                "Unable to resolve a @sample link: \"$fullyQualifiedLink\". Is it used correctly? " +
                        "Expecting a link to a reachable (resolvable) top-level Kotlin function."
            )
            return null
        } else if (psiElement.language != KotlinLanguage.INSTANCE) {
            dokkaLogger.warn("Unable to resolve non-Kotlin @sample links: \"$fullyQualifiedLink\"")
            return null
        } else if (psiElement !is KtFunction) {
            dokkaLogger.warn("Unable to process a @sample link: \"$fullyQualifiedLink\". Only function links allowed.")
            return null
        }

        val imports = processImports(psiElement, sampleRewriter)
        val links = mutableListOf<DRI>()
        val body = analyze(samplesKotlinAnalysis.getModuleOrNull(sourceSet)!!) {
            processBody(psiElement, this, links)
        }

        return SampleSnippet(imports, body, links)
    }

    private fun findPsiElement(sourceSet: DokkaSourceSet, fqLink: String): PsiElement? {
        // fallback to default roots of the source set even if sample roots are assigned,
        // because `@sample` tag can contain links to functions from project sources
        return samplesKotlinAnalysis.findPsiElement(sourceSet, fqLink)
            ?: projectKotlinAnalysis.findPsiElement(sourceSet, fqLink)
    }

    private fun KotlinAnalysis.findPsiElement(sourceSet: DokkaSourceSet, fqLink: String): PsiElement? {
        val ktSourceModule = this.getModuleOrNull(sourceSet) ?: return null
        return analyze(ktSourceModule) {
            resolveKDocTextLinkToSymbol(fqLink)
                ?.sourcePsiSafe()
        }
    }

    private fun processImports(psiElement: PsiElement, sampleRewriter: SampleRewriter?): List<String> {
        val psiFile = psiElement.containingFile
        val importsList = (psiFile as? KtFile)?.importList ?: return emptyList()
        return importsList.imports
            .map { it.text.removePrefix("import ") }
            .filter { it.isNotBlank() }
            .applyIf(sampleRewriter != null) {
                mapNotNull { sampleRewriter?.rewriteImportDirective(it) }
            }
    }

    private fun processBody(sampleElement: KtDeclarationWithBody, session: KaSession, links: MutableList<DRI>): String {
        return getSampleBody(sampleElement, session, links)
            .trim { it == '\n' || it == '\r' }
            .trimEnd()
            .trimIndent()
    }

    private fun getSampleBody(psiElement: KtDeclarationWithBody, session: KaSession, links: MutableList<DRI>): String {
        val bodyExpression = psiElement.bodyExpression
        val bodyExpressionText = bodyExpression!!.buildSampleText(session, links)
        return when (bodyExpression) {
            is KtBlockExpression -> bodyExpressionText.removeSurrounding("{", "}") // without braces according to the documentation of [SampleSnippet.body]
            else -> bodyExpressionText
        }
    }

    private fun PsiElement.buildSampleText(session: KaSession, links: MutableList<DRI>): String {
        val textBuilder = StringBuilder()
        val errors = mutableListOf<SampleBuilder.ConvertError>()

        this.accept(SampleBuilder(sampleRewriter, textBuilder, errors, links, session))

        errors.forEach {
            val st = it.e.stackTraceToString()

            dokkaLogger.warn("Exception thrown while sample rewriting at ${containingFile.name}: (${it.loc})\n```\n${it.text}\n```\n$st")
        }
        return textBuilder.toString()
    }

    override fun close() {
        samplesKotlinAnalysis.close()
    }
}

private class SampleBuilder(
    private val sampleRewriter: SampleRewriter?,
    val textBuilder: StringBuilder,
    val errors: MutableList<ConvertError>,
    val links: MutableList<DRI>,
    val session: KaSession
) : KtTreeVisitorVoid() {

    data class ConvertError(val e: Exception, val text: String, val loc: String)

    override fun visitCallExpression(expression: KtCallExpression) {
        val callRewriter = expression.calleeExpression?.text?.let { sampleRewriter?.getFunctionCallRewriter(it) }
        if(callRewriter != null) {
            val rewrittenResult = callRewriter.rewrite(
                arguments = expression.valueArguments.map { it.text ?: "" }, // expect not nullable ASTDelegatePsiElement.text
                typeArguments = expression.typeArguments.map { it.text ?: "" } // expect not nullable ASTDelegatePsiElement.text
            )

            textBuilder.append(rewrittenResult)
        } else {
            super.visitCallExpression(expression)
        }
    }

    private fun reportProblemConvertingElement(element: PsiElement, e: Exception) {
        val text = element.text
        val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)

        val lineInfo = if (document != null) {
            val lineNumber = document.getLineNumber(element.startOffset)
            "$lineNumber, ${element.startOffset - document.getLineStartOffset(lineNumber)}"
        } else {
            "offset: ${element.startOffset}"
        }
        errors += ConvertError(e, text, lineInfo)
    }

    override fun visitElement(element: PsiElement) {
        if (element is LeafPsiElement) {
            textBuilder.append(element.text)
            return
        }

        element.acceptChildren(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                try {
                    element.accept(this@SampleBuilder)
                } catch (e: Exception) {
                    try {
                        reportProblemConvertingElement(element, e)
                    } finally {
                        textBuilder.append(element.text) //recover
                    }
                }
            }
        })
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression): Unit = with(session) {
        // just named calls, no arguments
        if (expression !is KtNameReferenceExpression) return super.visitReferenceExpression(expression)

        val dri = resolveDRIFromExpression(expression) ?: return super.visitReferenceExpression(expression)

        // println("${expression.text} -> $dri")

        val index = links.size
        links.add(dri)

        textBuilder.append("%$index%")
        super.visitReferenceExpression(expression)
        textBuilder.append("%$index%")
    }

    // best effort resolution
    private fun KaSession.resolveDRIFromExpression(expression: KtReferenceExpression): DRI? {
        if (expression !is KtNameReferenceExpression) return null

        val callInfo = expression.resolveToCall() as? KaSuccessCallInfo ?: return null

        return when (val call = callInfo.call) {
            is KaFunctionCall<*> -> getDRIFromFunction(call.symbol)
            is KaVariableAccessCall if call.symbol.callableId != null -> getDRIFromVariable(call.symbol)
            else -> null
        }
    }
}
