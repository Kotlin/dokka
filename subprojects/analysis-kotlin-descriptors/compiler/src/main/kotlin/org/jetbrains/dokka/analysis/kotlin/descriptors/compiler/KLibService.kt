package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.kotlin.library.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.storage.StorageManager

@InternalDokkaApi
interface KLibService {
    fun KotlinLibrary.createPackageFragmentProvider(
        storageManager: StorageManager,
        metadataModuleDescriptorFactory: KlibMetadataModuleDescriptorFactory,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptor,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider?

    fun isAnalysisCompatible(kotlinLibrary: KotlinLibrary): Boolean
}
