package org.jetbrains.dokka.analysis

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.links.TypeReference
import org.jetbrains.kotlin.descriptors.CallableDescriptor

fun Callable.Companion.from(descriptor: CallableDescriptor, name: String? = null) = with(descriptor) {
    Callable(
        name ?: descriptor.name.asString(),
        extensionReceiverParameter?.let { TypeReference.from(it) },
        valueParameters.mapNotNull { TypeReference.from(it) }
    )
}

fun Callable.Companion.from(psi: PsiMethod) = with(psi) {
    Callable(
        name,
        null,
        parameterList.parameters.map { param -> JavaClassReference(param.type.canonicalText) })
}

fun Callable.Companion.from(psi: PsiField): Callable {
    return Callable(
        name = psi.name,
        receiver = null,
        params = emptyList()
    )
}
