package org.jetbrains.dokka.analysis

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.idea.klib.createKlibPackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService

/** TODO: replace by [NativeResolverForModuleFactory] after fix of KT-40734 */
internal class DokkaNativeResolverForModuleFactory(
    private val targetEnvironment: TargetEnvironment
) : ResolverForModuleFactory() {
    companion object {
        private val metadataFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)
    }

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings
    ): ResolverForModule {

        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            moduleContext.project,
            moduleContext.storageManager,
            moduleContent.syntheticFiles,
            moduleContent.moduleContentScope,
            moduleContent.moduleInfo
        )

        val container = createContainerForLazyResolve(
            moduleContext,
            declarationProviderFactory,
            CodeAnalyzerInitializer.getInstance(moduleContext.project).createTrace(),
            moduleDescriptor.platform!!,
            NativePlatformAnalyzerServices,
            targetEnvironment,
            languageVersionSettings
        )

        var packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

        val klibPackageFragmentProvider = (moduleContent.moduleInfo as? DokkaNativeKlibLibraryInfo)
            ?.kotlinLibrary
            ?.createKlibPackageFragmentProvider(
                storageManager = moduleContext.storageManager,
                metadataModuleDescriptorFactory = metadataFactories.DefaultDeserializedDescriptorFactory,
                languageVersionSettings = languageVersionSettings,
                moduleDescriptor = moduleDescriptor,
                lookupTracker = LookupTracker.DO_NOTHING
            )

        if (klibPackageFragmentProvider != null) {
            packageFragmentProvider =
                CompositePackageFragmentProvider(listOf(packageFragmentProvider, klibPackageFragmentProvider))
        }

        return ResolverForModule(packageFragmentProvider, container)
    }
}
