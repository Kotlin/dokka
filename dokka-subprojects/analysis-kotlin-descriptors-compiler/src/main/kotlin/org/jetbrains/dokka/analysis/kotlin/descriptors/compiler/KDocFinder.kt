/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

@InternalDokkaApi
public interface KDocFinder {
    public fun KtElement.findKDoc(): KDocTag?

    public fun DeclarationDescriptor.find(
        descriptorToPsi: (DeclarationDescriptorWithSource) -> PsiElement? = {
            DescriptorToSourceUtils.descriptorToDeclaration(
                it
            )
        }
    ): KDocTag?

    public fun resolveKDocLink(
        fromDescriptor: DeclarationDescriptor,
        qualifiedName: String,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        emptyBindingContext: Boolean = false
    ): Collection<DeclarationDescriptor>
}
