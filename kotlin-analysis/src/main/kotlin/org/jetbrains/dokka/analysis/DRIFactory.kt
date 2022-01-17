package org.jetbrains.dokka.analysis

import com.intellij.psi.*
import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun DRI.Companion.from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
    val parameter = firstIsInstanceOrNull<ValueParameterDescriptor>()
    val callable = parameter?.containingDeclaration ?: firstIsInstanceOrNull<CallableDescriptor>()

    DRI(
        packageName = firstIsInstanceOrNull<PackageFragmentDescriptor>()?.fqName?.asString() ?: "",
        classNames = (filterIsInstance<ClassDescriptor>() + filterIsInstance<TypeAliasDescriptor>()).toList()
            .takeIf { it.isNotEmpty() }
            ?.asReversed()
            ?.joinToString(separator = ".") { it.name.asString() },
        callable = callable?.let { Callable.from(it) },
        target = DriTarget.from(parameter ?: descriptor),
        extra = if (descriptor is EnumEntrySyntheticClassDescriptor || descriptor.safeAs<ClassDescriptor>()?.kind == ClassKind.ENUM_ENTRY)
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        else null
    )
}

fun DRI.Companion.from(psi: PsiElement) = psi.parentsWithSelf.run {
    val psiMethod = firstIsInstanceOrNull<PsiMethod>()
    val psiField = firstIsInstanceOrNull<PsiField>()
    val classes = filterIsInstance<PsiClass>().filterNot { it is PsiTypeParameter }
        .toList() // We only want exact PsiClass types, not PsiTypeParameter subtype
    DRI(
        packageName = classes.lastOrNull()?.qualifiedName?.substringBeforeLast('.', "") ?: "",
        classNames = (if (psi is PsiEnumConstant) listOfNotNull(psiField?.name) + classes.toList().mapNotNull { it.name } else classes.toList().mapNotNull { it.name })
            .takeIf { it.isNotEmpty() }?.asReversed()
            ?.joinToString("."),
        callable = psiMethod?.let { Callable.from(it) } ?: psiField?.takeUnless { psi is PsiEnumConstant }?.let { Callable.from(it) },
        target = DriTarget.from(psi),
        extra = if (psi is PsiEnumConstant)
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        else null
    )
}
