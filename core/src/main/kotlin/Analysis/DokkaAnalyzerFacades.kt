package org.jetbrains.dokka.Analysis

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.caches.project.LibraryModuleInfo
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.ide.konan.KOTLIN_NATIVE_CURRENT_ABI_VERSION
import org.jetbrains.kotlin.ide.konan.createPackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.createKotlinJavascriptPackageFragmentProvider
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils

fun createContainerForLazyResolve(
    moduleContext: ModuleContext,
    declarationProviderFactory: DeclarationProviderFactory,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    targetPlatformVersion: TargetPlatformVersion,
    targetEnvironment: TargetEnvironment,
    languageVersionSettings: LanguageVersionSettings
): StorageComponentContainer = createContainer("LazyResolve", platform) {
    configureModule(moduleContext, platform, targetPlatformVersion, bindingTrace)

    useInstance(declarationProviderFactory)
    useInstance(languageVersionSettings)

    useImpl<AnnotationResolverImpl>()
    useImpl<CompilerDeserializationConfiguration>()
    targetEnvironment.configure(this)

    useImpl<ResolveSession>()
    useImpl<LazyTopDownAnalyzer>()
}


object DokkaJsAnalyzerFacade : ResolverForModuleFactory() {
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        targetPlatformVersion: TargetPlatformVersion
    ): ResolverForModule {
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project,
            moduleContext.storageManager,
            syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val container = createContainerForLazyResolve(
            moduleContext,
            declarationProviderFactory,
            BindingTraceContext(),
            JsPlatform,
            TargetPlatformVersion.NoVersion,
            targetEnvironment,
            languageVersionSettings
        )
        var packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

        if (moduleInfo is LibraryModuleInfo && moduleInfo.platform == JsPlatform) {
            val providers = moduleInfo.getLibraryRoots()
                .flatMap { KotlinJavascriptMetadataUtils.loadMetadata(it) }
                .filter { it.version.isCompatible() }
                .map { metadata ->
                    val (header, packageFragmentProtos) =
                        KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
                    createKotlinJavascriptPackageFragmentProvider(
                        moduleContext.storageManager, moduleDescriptor, header, packageFragmentProtos, metadata.version,
                        container.get(), LookupTracker.DO_NOTHING
                    )
                }

            if (providers.isNotEmpty()) {
                packageFragmentProvider = CompositePackageFragmentProvider(listOf(packageFragmentProvider) + providers)
            }
        }

        return ResolverForModule(packageFragmentProvider, container)
    }

    override val targetPlatform: TargetPlatform
        get() = JsPlatform
}

object DokkaNativeAnalyzerFacade : ResolverForModuleFactory() {
    override val targetPlatform: TargetPlatform
        get() = KonanPlatform

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        targetPlatformVersion: TargetPlatformVersion
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
            BindingTraceContext(),
            targetPlatform,
            TargetPlatformVersion.NoVersion,
            targetEnvironment,
            languageVersionSettings
        )

        val packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider
        val fragmentProviders = mutableListOf(packageFragmentProvider)

        val moduleInfo = moduleContent.moduleInfo

        if (moduleInfo is LibraryModuleInfo) {
            moduleInfo.getLibraryRoots()
                .filter { File(it).extension != "jar" }
                .map { createKonanLibrary(File(it), KOTLIN_NATIVE_CURRENT_ABI_VERSION) }
                .mapTo(fragmentProviders) {
                    it.createPackageFragmentProvider(
                        moduleContext.storageManager,
                        languageVersionSettings,
                        moduleDescriptor
                    )
                }

        }

        return ResolverForModule(CompositePackageFragmentProvider(fragmentProviders), container)
    }
}
