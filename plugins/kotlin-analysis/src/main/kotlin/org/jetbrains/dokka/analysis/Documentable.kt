package org.jetbrains.dokka.analysis

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement

class DescriptorDocumentableSource(val descriptor: DeclarationDescriptor) : DocumentableSource {
    override val path = descriptor.toSourceElement.containingFile.toString()
}

class PsiDocumentableSource(val psi: PsiNamedElement) : DocumentableSource {
    override val path = psi.containingFile.virtualFile.path
}
