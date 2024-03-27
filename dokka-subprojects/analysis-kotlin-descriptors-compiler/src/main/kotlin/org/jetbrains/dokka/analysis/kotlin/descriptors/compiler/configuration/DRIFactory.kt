/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration

import com.intellij.psi.*
import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal fun DRI.Companion.from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
    if (descriptor is PackageViewDescriptor) {
        return@run DRI(
            packageName = descriptor.fqName.asString()
        )
    }
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
        extra = if (descriptor is EnumEntrySyntheticClassDescriptor || (descriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_ENTRY)
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        else null
    )
}

internal fun DRI.Companion.from(psi: PsiElement) = psi.parentsWithSelf.run {
    val psiMethod = firstIsInstanceOrNull<PsiMethod>()
    val psiField = firstIsInstanceOrNull<PsiField>()
    val classes = filterIsInstance<PsiClass>().filterNot { it is PsiTypeParameter }
        .toList() // We only want exact PsiClass types, not PsiTypeParameter subtype
    val additionalClasses = if (psi is PsiEnumConstant) listOfNotNull(psiField?.name) else emptyList()
    DRI(
        packageName = classes.lastOrNull()?.qualifiedName?.substringBeforeLast('.', "") ?: "",
        classNames = (additionalClasses + classes.mapNotNull { it.name }).takeIf { it.isNotEmpty() }
            ?.asReversed()?.joinToString("."),
        // The fallback strategy test whether psi is not `PsiEnumConstant`. The reason behind this is that
        // we need unified DRI for both Java and Kotlin enums, so we can link them properly and treat them alike.
        // To achieve that, we append enum name to classNames list and leave the callable part set to null. For Kotlin enums
        // it is by default, while for Java enums we have to explicitly test for that in this `takeUnless` condition.
        callable = psiMethod?.let { Callable.from(it) } ?: psiField?.takeUnless { psi is PsiEnumConstant }?.let { Callable.from(it) },
        target = DriTarget.from(psi),
        extra = if (psi is PsiEnumConstant)
            DRIExtraContainer().also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
        else null
    )
}
