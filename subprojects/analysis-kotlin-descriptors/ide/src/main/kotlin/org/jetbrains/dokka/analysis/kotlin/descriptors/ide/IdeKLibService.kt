package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KLibService
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.idea.klib.createKlibPackageFragmentProvider
import org.jetbrains.kotlin.idea.klib.getCompatibilityInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.storage.StorageManager

internal class IdeKLibService : KLibService {
    override fun KotlinLibrary.createPackageFragmentProvider(
        storageManager: StorageManager,
        metadataModuleDescriptorFactory: KlibMetadataModuleDescriptorFactory,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptor,
        lookupTracker: LookupTracker,
    ): PackageFragmentProvider? {
        return this.createKlibPackageFragmentProvider(
            storageManager, metadataModuleDescriptorFactory, languageVersionSettings, moduleDescriptor, lookupTracker
        )
    }

    override fun isAnalysisCompatible(kotlinLibrary: KotlinLibrary): Boolean {
        return kotlinLibrary.getCompatibilityInfo().isCompatible
    }
}
