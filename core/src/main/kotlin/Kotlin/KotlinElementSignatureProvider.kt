package org.jetbrains.dokka

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiNameIdentifierOwner
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

        val desc = when (forPsi) {
            is KtLightElement<*, *> -> return signature(forPsi.kotlinOrigin!!)
            is PsiPackage -> resolutionFacade.moduleDescriptor.getPackage(FqName(forPsi.qualifiedName))
            is PsiMember -> forPsi.getJavaOrKotlinMemberDescriptor(resolutionFacade)
            else -> resolutionFacade.resolveSession.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, forPsi]
        }

        if (desc == null && (forPsi as? PsiNameIdentifierOwner)?.name == "remove") {
            (forPsi as? PsiMember)?.getJavaOrKotlinMemberDescriptor(resolutionFacade)
        }

        return desc?.let { signature(it) }
                ?: run { "no desc for $forPsi in ${(forPsi as? PsiMember)?.containingClass}" }
    }

    override fun signature(forDesc: DeclarationDescriptor): String = forDesc.signature()
}