package org.jetbrains.dokka.analysis

import com.intellij.psi.*
import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun DRI.Companion.from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
    val parameter = firstIsInstanceOrNull<ValueParameterDescriptor>()
    val callable = parameter?.containingDeclaration ?: firstIsInstanceOrNull<CallableDescriptor>()
    DRI(
        packageName = firstIsInstanceOrNull<PackageFragmentDescriptor>()?.fqName?.asString() ?: "",
        classNames = (filterIsInstance<ClassDescriptor>().map {
                if (it.kind == ClassKind.ENUM_ENTRY)
                    it.name.asString().split(".").dropLast(1).joinToString(".")
                else
                    it.name.asString()
                } + filterIsInstance<TypeAliasDescriptor>().map { it.name.asString() }
            ).toList()
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.asReversed()
            ?.joinToString(separator = "."),
        callable = callable?.let { Callable.from(it) }
            ?: descriptor.safeAs<LazyClassDescriptor>().takeIf { it?.kind == ClassKind.ENUM_ENTRY }?.let { Callable.from(it) }
            ?: descriptor.safeAs<EnumEntrySyntheticClassDescriptor>()?.let { Callable.from(it) },
        target = DriTarget.from(parameter ?: descriptor),
        extra = if (descriptor is EnumEntrySyntheticClassDescriptor)
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
        classNames = classes.toList().takeIf { it.isNotEmpty() }?.asReversed()?.mapNotNull { it.name }
            ?.joinToString("."),
        callable = psiMethod?.let { Callable.from(it) } ?: psiField?.let { Callable.from(it) },
        target = DriTarget.from(psi),
        extra = if (psi is PsiEnumConstant)
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        else null
    )
}
