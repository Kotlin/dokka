package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.DescriptorFinder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

internal class IdeDescriptorFinder : DescriptorFinder {
    override fun KtDeclaration.findDescriptor(): DeclarationDescriptor? {
        return this.descriptor
    }
    val KtDeclaration.descriptor: DeclarationDescriptor?
        get() = if (this is KtParameter) this.descriptor else this.resolveToDescriptorIfAny(BodyResolveMode.FULL)
}
