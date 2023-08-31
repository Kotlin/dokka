/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KDocFinder
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

internal class IdePluginKDocFinder(
    private val context: DokkaContext
) : KDocFinder {

    override fun KtElement.findKDoc(): KDocTag? {
        return this.findKDoc { DescriptorToSourceUtils.descriptorToDeclaration(it) }?.contentTag
    }

    override fun DeclarationDescriptor.find(descriptorToPsi: (DeclarationDescriptorWithSource) -> PsiElement?): KDocTag? {
        return this.findKDoc(descriptorToPsi)?.contentTag
    }

    override fun resolveKDocLink(
        fromDescriptor: DeclarationDescriptor,
        qualifiedName: String,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        emptyBindingContext: Boolean
    ): Collection<DeclarationDescriptor> {
        val facadeAnalysisContext = context
            .plugin<CompilerDescriptorAnalysisPlugin>()
            .querySingle { kotlinAnalysis }[sourceSet] as ResolutionFacadeAnalysisContext

        return org.jetbrains.kotlin.idea.kdoc.resolveKDocLink(
            context = if (emptyBindingContext) BindingContext.EMPTY else facadeAnalysisContext.resolveSession.bindingContext,
            resolutionFacade = facadeAnalysisContext.facade,
            fromDescriptor = fromDescriptor,
            fromSubjectOfTag = null,
            qualifiedName = qualifiedName.split('.'),
            contextElement = fromDescriptor.findPsi()
        )
    }
}
