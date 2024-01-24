/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration

@InternalDokkaApi
public interface DescriptorFinder {
    public fun KtDeclaration.findDescriptor(): DeclarationDescriptor?
}
