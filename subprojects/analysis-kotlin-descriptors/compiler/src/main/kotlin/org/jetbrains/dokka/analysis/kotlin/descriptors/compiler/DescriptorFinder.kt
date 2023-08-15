package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration

@InternalDokkaApi
interface DescriptorFinder {
    fun KtDeclaration.findDescriptor(): DeclarationDescriptor?
}
