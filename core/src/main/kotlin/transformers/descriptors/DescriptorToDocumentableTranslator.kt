package org.jetbrains.dokka.transformers.descriptors

import org.jetbrains.dokka.model.DModuleView
import org.jetbrains.dokka.model.DPass
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

interface DescriptorToDocumentableTranslator {
    fun invoke(
        moduleName: String,
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData
    ): DPass
}