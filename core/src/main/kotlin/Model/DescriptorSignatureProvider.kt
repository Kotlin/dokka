package org.jetbrains.dokka.Model

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

interface DescriptorSignatureProvider {
    fun signature(forDesc: DeclarationDescriptor): String
}