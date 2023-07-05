package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.DescriptorFinder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtDeclaration

internal class IdeDescriptorFinder : DescriptorFinder {
    override fun KtDeclaration.findDescriptor(): DeclarationDescriptor? {
        return this.descriptor
    }
}
