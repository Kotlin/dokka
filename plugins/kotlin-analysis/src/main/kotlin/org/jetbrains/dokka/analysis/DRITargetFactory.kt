package org.jetbrains.dokka.analysis

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.dokka.links.DriTarget
import org.jetbrains.dokka.links.PointingToCallableParameters
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.links.PointingToGenericParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun DriTarget.Companion.from(descriptor: DeclarationDescriptor): DriTarget = descriptor.parentsWithSelf.run {
    return when (descriptor) {
        is TypeParameterDescriptor -> PointingToGenericParameters(descriptor.index)
        else -> {
            val callable = firstIsInstanceOrNull<CallableDescriptor>()
            val params =
                callable?.let { listOfNotNull(it.extensionReceiverParameter) + it.valueParameters }.orEmpty()
            val parameterDescriptor = firstIsInstanceOrNull<ParameterDescriptor>()

            parameterDescriptor?.let { PointingToCallableParameters(params.indexOf(it)) }
                ?: PointingToDeclaration
        }
    }
}


fun DriTarget.Companion.from(psi: PsiElement): DriTarget = psi.parentsWithSelf.run {
    return when (psi) {
        is PsiTypeParameter -> PointingToGenericParameters(psi.index)
        else -> firstIsInstanceOrNull<PsiParameter>()?.let {
            val callable = firstIsInstanceOrNull<PsiMethod>()
            val params = (callable?.parameterList?.parameters).orEmpty()
            PointingToCallableParameters(params.indexOf(it))
        } ?: PointingToDeclaration
    }
}
