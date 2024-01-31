/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl

import com.intellij.psi.PsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KDocFinder
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

internal class DescriptorSampleAnalysisEnvironmentCreator(
    private val context: DokkaContext,
) : SampleAnalysisEnvironmentCreator {

    private val descriptorAnalysisPlugin = context.plugin<CompilerDescriptorAnalysisPlugin>()

    override fun <T> use(block: SampleAnalysisEnvironment.() -> T): T {
        // Run from the thread of Dispatchers.Default as it can help
        // avoid memory leaks through the compiler's ThreadLocals.
        // Might not be relevant if the project stops using coroutines.
        return runBlocking(Dispatchers.Default) {
            @OptIn(DokkaPluginApiPreview::class)
            SamplesKotlinAnalysis(
                sourceSets = context.configuration.sourceSets,
                context = context,
                projectKotlinAnalysis = descriptorAnalysisPlugin.querySingle { kotlinAnalysis }
            ).use { kotlinAnalysis ->
                val sampleAnalysis = DescriptorSampleAnalysisEnvironment(
                    kdocFinder = descriptorAnalysisPlugin.querySingle { kdocFinder },
                    kotlinAnalysis = kotlinAnalysis,
                    dokkaLogger = context.logger
                )
                block(sampleAnalysis)
            }
        }
    }
}

internal class DescriptorSampleAnalysisEnvironment(
    private val kdocFinder: KDocFinder,
    private val kotlinAnalysis: KotlinAnalysis,
    private val dokkaLogger: DokkaLogger,
) : SampleAnalysisEnvironment {

    override fun resolveSample(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        fullyQualifiedLink: String,
    ): SampleSnippet? {
        val resolveSession = kotlinAnalysis[sourceSet].resolveSession

        val samplePsiElement = resolveSession.resolveSamplePsiElement(sourceSet, fullyQualifiedLink)
        if (samplePsiElement == null) {
            dokkaLogger.debug("Cannot resolve sample element for: \"$fullyQualifiedLink\"")
            return null
        } else if (samplePsiElement.containingFile !is KtFile) {
            dokkaLogger.warn("Unable to resolve non-Kotlin @sample links: \"$fullyQualifiedLink\"")
            return null
        }

        return SampleSnippet(
            imports = processImports(samplePsiElement),
            body = processBody(samplePsiElement)
        )
    }

    private fun ResolveSession.resolveSamplePsiElement(
        dokkaSourceSet: DokkaConfiguration.DokkaSourceSet,
        fqLink: String,
    ): PsiElement? {
        val packageDescriptor = resolveNearestPackageDescriptor(fqLink)
        if (packageDescriptor == null) {
            dokkaLogger.debug(
                "Unable to resolve package descriptor for @sample: \"$fqLink\";"
            )
            return null
        }

        val kdocLink = kdocFinder.resolveKDocLink(
            fromDescriptor = packageDescriptor,
            qualifiedName = fqLink,
            sourceSet = dokkaSourceSet,
            emptyBindingContext = true
        ).firstOrNull()

        if (kdocLink == null) {
            dokkaLogger.warn(
                "Unable to resolve a @sample link: \"$fqLink\". Is it used correctly? " +
                        "Expecting a link to a reachable (resolvable) Kotlin function."
            )
            return null
        } else if (kdocLink.toSourceElement !is KotlinSourceElement) {
            dokkaLogger.warn("Unable to resolve non-Kotlin @sample links: \"$fqLink\"")
            return null
        } else if (kdocLink !is FunctionDescriptor) {
            dokkaLogger.warn("Unable to process a @sample link: \"$fqLink\". Only function links allowed.")
            return null
        }
        return DescriptorToSourceUtils.descriptorToDeclaration(kdocLink)
    }

    /**
     * Tries to resolve [fqLink]'s package.
     *
     * Since [fqLink] can be both a link to a top-level function and a link to a function within a class,
     * we cannot tell for sure if [fqLink] contains a class name or not (relying on case letters is error-prone,
     * there are exceptions). But we know for sure that the last element in the link is the function.
     *
     * So we start with what we think is the deepest package path, and if we cannot find a package descriptor
     * for it - we drop one level and try again, until we find something or reach root.
     *
     * This function should also account for links to declarations within the root package (`""`).
     *
     * Here are some examples:
     *
     * Given [fqLink] = `com.example.ClassName.functionName`:
     * 1) First pass, trying to resolve package `com.example.ClassName`. Failure.
     * 2) Second pass, trying to resolve package `com.example`. Success.
     *
     * Given [fqLink] = `com.example.functionName`:
     * 1) First pass, trying to resolve package `com.example`. Success.
     *
     * Given [fqLink] = `ClassName.functionName` (root package):
     * 1) First pass, trying to resolve package `ClassName`. Failure.
     * 2) Second pass, trying to resolve package `""`. Success.
     */
    private fun ResolveSession.resolveNearestPackageDescriptor(fqLink: String): LazyPackageDescriptor? {
        val isRootPackage = !fqLink.contains('.')
        val supposedPackageName = if (isRootPackage) "" else fqLink.substringBeforeLast(".")

        val packageDescriptor = this.getPackageFragment(FqName(supposedPackageName))
        if (packageDescriptor != null) {
            return packageDescriptor
        }
        dokkaLogger.debug("Failed to resolve package \"$supposedPackageName\" for sample \"$fqLink\"")

        if (isRootPackage) {
            // cannot go any deeper
            return null
        }

        return resolveNearestPackageDescriptor(supposedPackageName.substringBeforeLast("."))
    }

    private fun processImports(sampleElement: PsiElement): List<String> {
        val psiFile = sampleElement.containingFile

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
