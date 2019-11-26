package org.jetbrains.dokka.transformers.descriptors

import org.jetbrains.dokka.Model.Module
import org.jetbrains.dokka.Model.Package
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

interface DescriptorToDocumentationTranslator {
    fun invoke(
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData,
        context: DokkaContext
    ): Module
}