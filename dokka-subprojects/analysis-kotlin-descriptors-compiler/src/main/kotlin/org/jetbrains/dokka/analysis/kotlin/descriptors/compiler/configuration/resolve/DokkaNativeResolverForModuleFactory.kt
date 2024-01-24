/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KLibService
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.SealedClassInheritorsProvider
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.scopes.optimization.OptimizingOptions

/** TODO: replace by [NativeResolverForModuleFactory] after fix of KT-40734 */
internal class DokkaNativeResolverForModuleFactory(
    private val targetEnvironment: TargetEnvironment,
    private val kLibService: KLibService,
) : ResolverForModuleFactory() {
    companion object {
        private val metadataFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)
    }

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {

        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            moduleContext.project,
            moduleContext.storageManager,
            moduleContent.syntheticFiles,
            moduleContent.moduleContentScope,
            moduleContent.moduleInfo
        )

        val container = createContainerForLazyResolve(
            moduleContext = moduleContext,
            declarationProviderFactory = declarationProviderFactory,
            bindingTrace = CodeAnalyzerInitializer.getInstance(moduleContext.project).createTrace(),
            platform = moduleDescriptor.platform!!,
            analyzerServices = NativePlatformAnalyzerServices,
            targetEnvironment = targetEnvironment,
            languageVersionSettings = languageVersionSettings,
            absentDescriptorHandlerClass = absentDescriptorHandlerClass
        )

        var packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

        val klibPackageFragmentProvider = with(kLibService) {
            (moduleContent.moduleInfo as? DokkaNativeKlibLibraryInfo)
                ?.kotlinLibrary
                ?.createPackageFragmentProvider(
                    storageManager = moduleContext.storageManager,
                    metadataModuleDescriptorFactory = metadataFactories.DefaultDeserializedDescriptorFactory,
                    languageVersionSettings = languageVersionSettings,
                    moduleDescriptor = moduleDescriptor,
                    lookupTracker = LookupTracker.DO_NOTHING
                )
        }

        if (klibPackageFragmentProvider != null) {
            packageFragmentProvider =
                CompositePackageFragmentProvider(listOf(packageFragmentProvider, klibPackageFragmentProvider), "DokkaCompositePackageFragmentProvider")
        }

        return ResolverForModule(packageFragmentProvider, container)
    }
}
