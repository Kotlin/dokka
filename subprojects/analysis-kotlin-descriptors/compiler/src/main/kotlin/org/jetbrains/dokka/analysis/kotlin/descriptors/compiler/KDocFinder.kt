package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

@InternalDokkaApi
interface KDocFinder {
    fun KtElement.findKDoc(): KDocTag?

    fun DeclarationDescriptor.find(
        descriptorToPsi: (DeclarationDescriptorWithSource) -> PsiElement? = {
            DescriptorToSourceUtils.descriptorToDeclaration(
                it
            )
        }
    ): KDocTag?

    fun resolveKDocLink(
        fromDescriptor: DeclarationDescriptor,
        qualifiedName: String,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        emptyBindingContext: Boolean = false
    ): Collection<DeclarationDescriptor>
}
