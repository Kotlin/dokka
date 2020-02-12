package org.jetbrains.dokka.transformers.descriptors

import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

interface DescriptorToDocumentationTranslator {
    fun invoke(
        moduleName: String,
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData
    ): Module
}