/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.DescriptorFinder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

internal class IdeDescriptorFinder : DescriptorFinder {
    override fun KtDeclaration.findDescriptor(): DeclarationDescriptor? {
        return if (this is KtParameter) this.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL) else this.resolveToDescriptorIfAny(
            BodyResolveMode.FULL
        )
    }

}


