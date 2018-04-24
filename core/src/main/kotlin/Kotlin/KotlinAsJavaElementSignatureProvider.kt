package org.jetbrains.dokka

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement

class KotlinAsJavaElementSignatureProvider : ElementSignatureProvider {

    private fun PsiElement.javaLikePsi() = when {
        this is KtElement -> toLightElements().firstOrNull()
        else -> this
    }

    override fun signature(forPsi: PsiElement): String {
        return getSignature(forPsi.javaLikePsi()) ?:
                "not implemented for $forPsi"
    }

    override fun signature(forDesc: DeclarationDescriptor): String {
        val sourcePsi = forDesc.sourcePsi()
        return getSignature(sourcePsi?.javaLikePsi()) ?:
                "not implemented for $forDesc with psi: $sourcePsi"
    }
}