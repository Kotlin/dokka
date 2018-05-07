package org.jetbrains.dokka

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import javax.inject.Inject

class KotlinElementSignatureProvider @Inject constructor(
        val resolutionFacade: DokkaResolutionFacade
) : ElementSignatureProvider {
    override fun signature(forPsi: PsiElement): String {
        return forPsi.extractDescriptor(resolutionFacade)
                ?.let { signature(it) }
                ?: run { "no desc for $forPsi in ${(forPsi as? PsiMember)?.containingClass}" }
    }

    override fun signature(forDesc: DeclarationDescriptor): String = forDesc.signature()
}


fun PsiElement.extractDescriptor(resolutionFacade: DokkaResolutionFacade): DeclarationDescriptor? {
    val forPsi = this

    return when (forPsi) {
        is KtLightElement<*, *> -> return (forPsi.kotlinOrigin!!).extractDescriptor(resolutionFacade)
        is PsiPackage -> resolutionFacade.moduleDescriptor.getPackage(FqName(forPsi.qualifiedName))
        is PsiMember -> forPsi.getJavaOrKotlinMemberDescriptor(resolutionFacade)
        else -> resolutionFacade.resolveSession.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, forPsi]
    }
}
