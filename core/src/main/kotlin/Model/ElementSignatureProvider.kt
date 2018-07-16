package org.jetbrains.dokka

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

interface ElementSignatureProvider {
    fun signature(forDesc: DeclarationDescriptor): String
    fun signature(forPsi: PsiElement): String
}