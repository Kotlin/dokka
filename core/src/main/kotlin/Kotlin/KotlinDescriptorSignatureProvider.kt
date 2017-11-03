package org.jetbrains.dokka.Kotlin

import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.signature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

class KotlinDescriptorSignatureProvider : DescriptorSignatureProvider {
    override fun signature(forDesc: DeclarationDescriptor): String = forDesc.signature()
}