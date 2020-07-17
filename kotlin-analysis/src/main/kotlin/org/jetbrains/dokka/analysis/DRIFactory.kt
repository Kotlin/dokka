package org.jetbrains.dokka.analysis

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun DRI.Companion.from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
    val callable = firstIsInstanceOrNull<CallableDescriptor>()
    DRI(
        firstIsInstanceOrNull<PackageFragmentDescriptor>()?.fqName?.asString(),
        (filterIsInstance<ClassDescriptor>() + filterIsInstance<TypeAliasDescriptor>()).toList()
            .takeIf { it.isNotEmpty() }
            ?.asReversed()
            ?.joinToString(separator = ".") { it.name.asString() },
        callable?.let { Callable.from(it) },
        DriTarget.from(descriptor)
    )
}

fun DRI.Companion.from(psi: PsiElement) = psi.parentsWithSelf.run {
    val psiMethod = firstIsInstanceOrNull<PsiMethod>()
    val psiField = firstIsInstanceOrNull<PsiField>()
    val classes = filterIsInstance<PsiClass>().toList()
    DRI(
        classes.lastOrNull()?.qualifiedName?.substringBeforeLast('.', ""),
        classes.toList().takeIf { it.isNotEmpty() }?.asReversed()?.mapNotNull { it.name }?.joinToString("."),
        psiMethod?.let { Callable.from(it) } ?: psiField?.let { Callable.from(it) },
        DriTarget.from(psi)
    )
}
