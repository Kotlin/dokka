package org.jetbrains.dokka.Kotlin

import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.getSignature
import org.jetbrains.dokka.sourcePsi
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement

class KotlinAsJavaDescriptorSignatureProvider : DescriptorSignatureProvider {
    override fun signature(forDesc: DeclarationDescriptor): String {
        val sourcePsi = forDesc.sourcePsi()
        val javaLikePsi = if (sourcePsi is KtElement) {
            sourcePsi.toLightElements().firstOrNull()
        } else {
            sourcePsi
        }

        return getSignature(javaLikePsi) ?:
                "not implemented for $forDesc with psi: $sourcePsi"
    }
}