// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter


@Suppress("ClassName")
internal sealed class CallType<TReceiver : KtElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    object DOT : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SAFE : CallType<KtExpression>(DescriptorKindFilter.ALL)

    object SUPER_MEMBERS : CallType<KtSuperExpression>(
        DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions exclude AbstractMembersExclude
    )

    object INFIX : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonInfixExclude)

    object OPERATOR : CallType<KtExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    class CallableReference(settings: LanguageVersionSettings) :
        CallType<KtExpression?>(DescriptorKindFilter.CALLABLES exclude LocalsAndSyntheticExclude(settings)) {
        override fun equals(other: Any?): Boolean = other is CallableReference
        override fun hashCode(): Int = javaClass.hashCode()
    }

    object IMPORT_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.ALL)

    object PACKAGE_DIRECTIVE : CallType<KtExpression?>(DescriptorKindFilter.PACKAGES)

    object TYPE : CallType<KtExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude DescriptorKindExclude.EnumEntry
    )

    object DELEGATE : CallType<KtExpression?>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    object ANNOTATION : CallType<KtExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude NonAnnotationClassifierExclude
    )

    private object NonInfixExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            !(descriptor is SimpleFunctionDescriptor && descriptor.isInfix)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonOperatorExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            !(descriptor is SimpleFunctionDescriptor && descriptor.isOperator)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private class LocalsAndSyntheticExclude(private val settings: LanguageVersionSettings) : DescriptorKindExclude() {
        // Currently, Kotlin doesn't support references to local variables
        // References to Java synthetic properties are supported only since Kotlin 1.9
        override fun excludes(descriptor: DeclarationDescriptor): Boolean  =
            descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED &&
                    !settings.supportsFeature(LanguageFeature.ReferencesToSyntheticJavaProperties)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonAnnotationClassifierExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is ClassifierDescriptor) return false
            return descriptor !is ClassDescriptor || descriptor.kind != ClassKind.ANNOTATION_CLASS
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    private object AbstractMembersExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableMemberDescriptor && descriptor.modality == Modality.ABSTRACT

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }
}
