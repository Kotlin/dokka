package org.jetbrains.dokka.analysis.resolve

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.idea.klib.createKlibPackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.SealedClassInheritorsProvider
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.createKotlinJavascriptPackageFragmentProvider
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File

/** TODO: replace by [org.jetbrains.kotlin.caches.resolve.JsResolverForModuleFactory] after fix of KT-40734 */
internal class DokkaJsResolverForModuleFactory(
    private val targetEnvironment: TargetEnvironment
) : ResolverForModuleFactory() {
    companion object {
        private val metadataFactories = KlibMetadataFactories({ DefaultBuiltIns.Instance }, DynamicTypeDeserializer)

        private val metadataModuleDescriptorFactory = KlibMetadataModuleDescriptorFactoryImpl(
            metadataFactories.DefaultDescriptorFactory,
            metadataFactories.DefaultPackageFragmentsFactory,
            metadataFactories.flexibleTypeDeserializer,
            metadataFactories.platformDependentTypeTransformer
        )
    }

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider
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
            CodeAnalyzerInitializer.getInstance(moduleContext.project).createTrace(), // BindingTraceContext(/* allowSliceRewrite = */ true),
            moduleDescriptor.platform!!,
            JsPlatformAnalyzerServices,
            targetEnvironment,
            languageVersionSettings
        )

        var packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

        val libraryProviders = createPackageFragmentProvider(moduleContent.moduleInfo, container, moduleContext, moduleDescriptor, languageVersionSettings)

        if (libraryProviders.isNotEmpty()) {
            packageFragmentProvider =
                CompositePackageFragmentProvider(listOf(packageFragmentProvider) + libraryProviders, "DokkaCompositePackageFragmentProvider")
        }
        return ResolverForModule(packageFragmentProvider, container)
    }

    internal fun <M : ModuleInfo> createPackageFragmentProvider(
        moduleInfo: M,
        container: StorageComponentContainer,
        moduleContext: ModuleContext,
        moduleDescriptor: ModuleDescriptorImpl,
        languageVersionSettings: LanguageVersionSettings
    ): List<PackageFragmentProvider> = when (moduleInfo) {
        is DokkaJsKlibLibraryInfo -> {
            listOfNotNull(
                moduleInfo.kotlinLibrary
                    .createKlibPackageFragmentProvider(
                        storageManager = moduleContext.storageManager,
                        metadataModuleDescriptorFactory = metadataModuleDescriptorFactory,
                        languageVersionSettings = languageVersionSettings,
                        moduleDescriptor = moduleDescriptor,
                        lookupTracker = LookupTracker.DO_NOTHING
                    )
            )
        }
        is LibraryModuleInfo -> {
            moduleInfo.getLibraryRoots()
                .flatMap {
                    if (File(it).exists()) {
                        KotlinJavascriptMetadataUtils.loadMetadata(it)
                    } else {
                        // TODO can/should we warn a user about a problem in a library root? If so how?
                        emptyList()
                    }
                }
                .filter { it.version.isCompatible() }
                .map { metadata ->
                    val (header, packageFragmentProtos) =
                        KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
                    createKotlinJavascriptPackageFragmentProvider(
                        moduleContext.storageManager, moduleDescriptor, header, packageFragmentProtos, metadata.version,
                        container.get(), LookupTracker.DO_NOTHING
                    )
                }
        }
        else -> emptyList()
    }
}