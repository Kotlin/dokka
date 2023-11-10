/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KLibService
import org.jetbrains.kotlin.library.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.idea.klib.CachingIdeKlibMetadataLoader
import org.jetbrains.kotlin.idea.klib.compatibilityInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
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
        return kotlinLibrary.compatibilityInfo.isCompatible
    }
}

internal fun KotlinLibrary.createKlibPackageFragmentProvider(
    storageManager: StorageManager,
    metadataModuleDescriptorFactory: KlibMetadataModuleDescriptorFactory,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor,
    lookupTracker: LookupTracker
): PackageFragmentProvider? {
    if (!compatibilityInfo.isCompatible) return null

    val packageFragmentNames = CachingIdeKlibMetadataLoader.loadModuleHeader(this).packageFragmentNameList

    return metadataModuleDescriptorFactory.createPackageFragmentProvider(
        library = this,
        packageAccessHandler = CachingIdeKlibMetadataLoader,
        packageFragmentNames = packageFragmentNames,
        storageManager = storageManager,
        moduleDescriptor = moduleDescriptor,
        configuration = CompilerDeserializationConfiguration(languageVersionSettings),
        compositePackageFragmentAddend = null,
        lookupTracker = lookupTracker
    )
}
