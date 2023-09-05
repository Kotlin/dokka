/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.NativeTypeTransformer
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.library.metadata.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

/**
 * Adapted from org.jetbrains.kotlin.cli.metadata.KlibMetadataDependencyContainer
 */
internal class DokkaKlibMetadataCommonDependencyContainer(
    kotlinLibraries: List<KotlinLibrary>,
    private val configuration: CompilerConfiguration,
    private val storageManager: StorageManager
) : CommonDependenciesContainer {

    private val builtIns
        get() = DefaultBuiltIns.Instance

    private val mutableDependenciesForAllModuleDescriptors = mutableListOf<ModuleDescriptorImpl>().apply {
        add(builtIns.builtInsModule)
    }

    private val mutableDependenciesForAllModules = mutableListOf<ModuleInfo>()

    private val moduleDescriptorsForKotlinLibraries: Map<KotlinLibrary, ModuleDescriptorImpl> =
        kotlinLibraries.associateBy({ it }) { library ->
            val moduleHeader = parseModuleHeader(library.moduleHeaderData)
            val moduleName = Name.special(moduleHeader.moduleName)
            val moduleOrigin = DeserializedKlibModuleOrigin(library)
            MetadataFactories.DefaultDescriptorFactory.createDescriptor(
                moduleName, storageManager, builtIns, moduleOrigin
            )
        }.also { result ->
            val resultValues = result.values
            resultValues.forEach { module ->
                module.setDependencies(mutableDependenciesForAllModuleDescriptors)
            }
            mutableDependenciesForAllModuleDescriptors.addAll(resultValues)
        }

    private val moduleInfosImpl: List<CommonKlibModuleInfo> = mutableListOf<CommonKlibModuleInfo>().apply {
        addAll(
            moduleDescriptorsForKotlinLibraries.map { (kotlinLibrary, moduleDescriptor) ->
                CommonKlibModuleInfo(moduleDescriptor.name, kotlinLibrary, mutableDependenciesForAllModules)
            }
        )
        mutableDependenciesForAllModules.addAll(this@apply)
    }

    override val moduleInfos: List<ModuleInfo> get() = moduleInfosImpl

    /* not used in Dokka */
    override val friendModuleInfos: List<ModuleInfo> = emptyList()

    /* not used in Dokka */
    override val refinesModuleInfos: List<ModuleInfo> = emptyList()

    override fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor {
        if (moduleInfo !in moduleInfos)
            error("Unknown module info $moduleInfo")

        // Ensure that the package fragment provider has been created and the module descriptor has been
        // initialized with the package fragment provider:
        packageFragmentProviderForModuleInfo(moduleInfo)

        return moduleDescriptorsForKotlinLibraries.getValue((moduleInfo as CommonKlibModuleInfo).kotlinLibrary)
    }

    override fun registerDependencyForAllModules(
        moduleInfo: ModuleInfo,
        descriptorForModule: ModuleDescriptorImpl
    ) {
        mutableDependenciesForAllModules.add(moduleInfo)
        mutableDependenciesForAllModuleDescriptors.add(descriptorForModule)
    }

    override fun packageFragmentProviderForModuleInfo(
        moduleInfo: ModuleInfo
    ): PackageFragmentProvider? {
        if (moduleInfo !in moduleInfos)
            return null
        return packageFragmentProviderForKotlinLibrary((moduleInfo as CommonKlibModuleInfo).kotlinLibrary)
    }

    private val klibMetadataModuleDescriptorFactory by lazy {
        KlibMetadataModuleDescriptorFactoryImpl(
            MetadataFactories.DefaultDescriptorFactory,
            MetadataFactories.DefaultPackageFragmentsFactory,
            MetadataFactories.flexibleTypeDeserializer,
            MetadataFactories.platformDependentTypeTransformer
        )
    }

    private fun packageFragmentProviderForKotlinLibrary(
        library: KotlinLibrary
    ): PackageFragmentProvider {
        val languageVersionSettings = configuration.languageVersionSettings

        val libraryModuleDescriptor = moduleDescriptorsForKotlinLibraries.getValue(library)
        val packageFragmentNames = parseModuleHeader(library.moduleHeaderData).packageFragmentNameList

        return klibMetadataModuleDescriptorFactory.createPackageFragmentProvider(
            library,
            packageAccessHandler = null,
            packageFragmentNames = packageFragmentNames,
            storageManager = LockBasedStorageManager("KlibMetadataPackageFragmentProvider"),
            moduleDescriptor = libraryModuleDescriptor,
            configuration = CompilerDeserializationConfiguration(languageVersionSettings),
            compositePackageFragmentAddend = null,
            lookupTracker = LookupTracker.DO_NOTHING
        ).also {
            libraryModuleDescriptor.initialize(it)
        }
    }
}

private val MetadataFactories =
    KlibMetadataFactories(
        { DefaultBuiltIns.Instance },
        NullFlexibleTypeDeserializer,
        NativeTypeTransformer()
    )
