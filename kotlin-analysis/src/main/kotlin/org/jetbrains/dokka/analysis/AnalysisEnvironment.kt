package org.jetbrains.dokka.analysis

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreModuleManager
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerationHandler
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.impl.source.javadoc.JavadocManagerImpl
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocManager
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.caches.resolve.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor
import org.jetbrains.kotlin.ide.konan.NativePlatformKindResolution
import org.jetbrains.kotlin.idea.klib.KlibLoadingMetadataCache
import org.jetbrains.kotlin.idea.klib.getCompatibilityInfo
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.unspecifiedJvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.jvm.JvmResolverForModuleFactory
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile


const val JAR_SEPARATOR = "!/"

/**
 * Kotlin as a service entry point
 *
 * Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode
 *
 * $messageCollector: required by compiler infrastructure and will receive all compiler messages
 * $body: optional and can be used to configure environment without creating local variable
 */
class AnalysisEnvironment(val messageCollector: MessageCollector, val analysisPlatform: Platform) : Disposable {
    val configuration = CompilerConfiguration()

    init {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    }

    fun createCoreEnvironment(): KotlinCoreEnvironment {
        System.setProperty("idea.io.use.nio2", "true")
        System.setProperty("idea.ignore.disabled.plugins", "true")

        val configFiles = when (analysisPlatform) {
            Platform.jvm, Platform.common -> EnvironmentConfigFiles.JVM_CONFIG_FILES
            Platform.native -> EnvironmentConfigFiles.NATIVE_CONFIG_FILES
            Platform.js -> EnvironmentConfigFiles.JS_CONFIG_FILES
        }

        val environment = KotlinCoreEnvironment.createForProduction(this, configuration, configFiles)
        val projectComponentManager = environment.project as MockComponentManager

        val projectFileIndex = CoreProjectFileIndex(
            environment.project,
            environment.configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)
        )

        val moduleManager = object : CoreModuleManager(environment.project, this) {
            override fun getModules(): Array<out Module> = arrayOf(projectFileIndex.module)
        }

        CoreApplicationEnvironment.registerComponentInstance(
            projectComponentManager.picoContainer,
            ModuleManager::class.java, moduleManager
        )

        CoreApplicationEnvironment.registerExtensionPoint(
            Extensions.getRootArea(),
            OrderEnumerationHandler.EP_NAME, OrderEnumerationHandler.Factory::class.java
        )

        CoreApplicationEnvironment.registerExtensionPoint(
            environment.project.extensionArea,
            JavadocTagInfo.EP_NAME, JavadocTagInfo::class.java
        )

        CoreApplicationEnvironment.registerExtensionPoint(
            Extensions.getRootArea(),
            CustomJavadocTagProvider.EP_NAME, CustomJavadocTagProvider::class.java
        )

        // TODO: figure out why compilation fails with unresolved `CoreApplicationEnvironment.registerApplicationService(...)`
        //  call, fix it appropriately
        with(ApplicationManager.getApplication() as MockApplication) {
            if (getService(KlibLoadingMetadataCache::class.java) == null)
                registerService(KlibLoadingMetadataCache::class.java, KlibLoadingMetadataCache())
        }

        projectComponentManager.registerService(
            ProjectFileIndex::class.java,
            projectFileIndex
        )

        projectComponentManager.registerService(
            ProjectRootManager::class.java,
            CoreProjectRootManager(projectFileIndex)
        )

        projectComponentManager.registerService(
            JavadocManager::class.java,
            JavadocManagerImpl(environment.project)
        )

        projectComponentManager.registerService(
            CustomJavadocTagProvider::class.java,
            CustomJavadocTagProvider { emptyList() }
        )

        registerExtensionPoint(
            ApplicationExtensionDescriptor("org.jetbrains.kotlin.idePlatformKind", IdePlatformKind::class.java),
            listOf(
                CommonIdePlatformKind,
                JvmIdePlatformKind,
                JsIdePlatformKind,
                NativeIdePlatformKind
            ),
            this
        )

        registerExtensionPoint(
            IdePlatformKindResolution,
            listOf(
                CommonPlatformKindResolution(),
                JvmPlatformKindResolution(),
                JsPlatformKindResolution(),
                NativePlatformKindResolution()
            ),
            this
        )

        return environment
    }

    private fun createSourceModuleSearchScope(project: Project, sourceFiles: List<KtFile>): GlobalSearchScope =
        when (analysisPlatform) {
            Platform.jvm -> TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            Platform.js, Platform.common, Platform.native -> GlobalSearchScope.filesScope(
                project,
                sourceFiles.map { it.virtualFile }.toSet()
            )
        }

    fun createResolutionFacade(environment: KotlinCoreEnvironment): Pair<DokkaResolutionFacade, DokkaResolutionFacade> {
        val projectContext = ProjectContext(environment.project, "Dokka")
        val sourceFiles = environment.getSourceFiles()

        val targetPlatform = when (analysisPlatform) {
            Platform.js -> JsPlatforms.defaultJsPlatform
            Platform.common -> CommonPlatforms.defaultCommonPlatform
            Platform.native -> NativePlatforms.unspecifiedNativePlatform
            Platform.jvm -> JvmPlatforms.defaultJvmPlatform
        }

        val nativeLibraries: Map<AbsolutePathString, LibraryModuleInfo> = loadNativeLibraries()

        val library = object : LibraryModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<library>")
            override val platform: TargetPlatform = targetPlatform
            override fun dependencies(): List<ModuleInfo> = listOf(this)
            override fun getLibraryRoots(): Collection<String> = classpath
                .map { libraryFile -> libraryFile.absolutePath }
                .filter { path -> path !in nativeLibraries }
        }

        val module = object : ModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<module>")
            override val platform: TargetPlatform = targetPlatform
            override fun dependencies(): List<ModuleInfo> = listOf(this, library) + nativeLibraries.values
        }

        val sourcesScope = createSourceModuleSearchScope(environment.project, sourceFiles)
        val modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo> = {
            when (it) {
                library -> ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                module -> ModuleContent(it, emptyList(), GlobalSearchScope.allScope(environment.project))
                is DokkaNativeKlibLibraryInfo -> {
                    if (it.libraryRoot in nativeLibraries)
                        ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                    else null
                }
                else -> null
            } ?: throw IllegalArgumentException("Unexpected module info")
        }

        var builtIns: JvmBuiltIns? = null

        val resolverForProject = when (analysisPlatform) {
            Platform.jvm -> {
                builtIns = JvmBuiltIns(
                    projectContext.storageManager,
                    JvmBuiltIns.Kind.FROM_CLASS_LOADER
                ) // TODO we should use FROM_DEPENDENCIES
                createJvmResolverForProject(
                    projectContext,
                    module,
                    library,
                    modulesContent,
                    sourcesScope,
                    builtIns
                )
            }
            Platform.common -> createCommonResolverForProject(
                projectContext,
                module,
                modulesContent,
                environment
            )
            Platform.js -> createJsResolverForProject(projectContext, module, modulesContent)
            Platform.native -> createNativeResolverForProject(projectContext, module, modulesContent)

        }
        val libraryModuleDescriptor = resolverForProject.descriptorForModule(library)
        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        builtIns?.initialize(moduleDescriptor, true)

        val resolverForLibrary =
            resolverForProject.resolverForModule(library) // Required before module to initialize library properly
        val resolverForModule = resolverForProject.resolverForModule(module)
        val libraryResolutionFacade =
            DokkaResolutionFacade(
                environment.project,
                libraryModuleDescriptor,
                resolverForLibrary
            )
        val created =
            DokkaResolutionFacade(
                environment.project,
                moduleDescriptor,
                resolverForModule
            )
        val projectComponentManager = environment.project as MockComponentManager
        projectComponentManager.registerService(
            KotlinCacheService::
            class.java,
            CoreKotlinCacheService(created)
        )

        return created to libraryResolutionFacade
    }

    private fun Platform.analyzerServices() = when (this) {
        Platform.js -> JsPlatformAnalyzerServices
        Platform.common -> CommonPlatformAnalyzerServices
        Platform.native -> NativePlatformAnalyzerServices
        Platform.jvm -> JvmPlatformAnalyzerServices
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun loadNativeLibraries(): Map<AbsolutePathString, LibraryModuleInfo> {
        if (analysisPlatform != Platform.native) return emptyMap()

        val dependencyResolver = DokkaNativeKlibLibraryDependencyResolver()
        val analyzerServices = analysisPlatform.analyzerServices()

        return buildMap {
            classpath.forEach { libraryFile ->
                val kotlinLibrary = resolveSingleFileKlib(
                    libraryFile = KFile(libraryFile.absolutePath),
                    strategy = ToolingSingleFileKlibResolveStrategy
                )

                if (kotlinLibrary.getCompatibilityInfo().isCompatible) {
                    // exists, is KLIB, has compatible format
                    put(
                        libraryFile.absolutePath,
                        DokkaNativeKlibLibraryInfo(kotlinLibrary, analyzerServices, dependencyResolver)
                    )
                }
            }
        }
    }

    private fun createCommonResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>,
        environment: KotlinCoreEnvironment
    ): ResolverForProject<ModuleInfo> {
        return object : AbstractResolverForProject<ModuleInfo>(
            "Dokka",
            projectContext,
            modules = module.dependencies()
        ) {
            override fun modulesContent(module: ModuleInfo): ModuleContent<ModuleInfo> = modulesContent(module)

            override fun builtInsForModule(module: ModuleInfo): KotlinBuiltIns = DefaultBuiltIns.Instance

            override fun createResolverForModule(
                descriptor: ModuleDescriptor,
                moduleInfo: ModuleInfo
            ): ResolverForModule =
                CommonResolverForModuleFactory(
                    CommonAnalysisParameters { content ->
                        environment.createPackagePartProvider(content.moduleContentScope)
                    },
                    CompilerEnvironment,
                    unspecifiedJvmPlatform,
                    true
                ).createResolverForModule(
                    descriptor as ModuleDescriptorImpl,
                    projectContext.withModule(descriptor),
                    modulesContent(moduleInfo),
                    this,
                    LanguageVersionSettingsImpl.DEFAULT
                )

            override fun sdkDependency(module: ModuleInfo): ModuleInfo? = null
        }
    }

    private fun createJsResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>
    ): ResolverForProject<ModuleInfo> {
        return object : AbstractResolverForProject<ModuleInfo>(
            "Dokka",
            projectContext,
            modules = module.dependencies()
        ) {
            override fun modulesContent(module: ModuleInfo): ModuleContent<ModuleInfo> = modulesContent(module)
            override fun createResolverForModule(
                descriptor: ModuleDescriptor,
                moduleInfo: ModuleInfo
            ): ResolverForModule = JsResolverForModuleFactory(
                CompilerEnvironment
            ).createResolverForModule(
                descriptor as ModuleDescriptorImpl,
                projectContext.withModule(descriptor),
                modulesContent(moduleInfo),
                this,
                LanguageVersionSettingsImpl.DEFAULT
            )

            override fun builtInsForModule(module: ModuleInfo): KotlinBuiltIns = DefaultBuiltIns.Instance

            override fun sdkDependency(module: ModuleInfo): ModuleInfo? = null
        }
    }

    private fun createNativeResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>
    ): ResolverForProject<ModuleInfo> {
        return object : AbstractResolverForProject<ModuleInfo>(
            "Dokka",
            projectContext,
            modules = module.dependencies()
        ) {
            override fun modulesContent(module: ModuleInfo): ModuleContent<ModuleInfo> = modulesContent(module)
            override fun createResolverForModule(
                descriptor: ModuleDescriptor,
                moduleInfo: ModuleInfo
            ): ResolverForModule {

                return DokkaNativeResolverForModuleFactory(CompilerEnvironment).createResolverForModule(
                    descriptor as ModuleDescriptorImpl,
                    projectContext.withModule(descriptor),
                    modulesContent(moduleInfo),
                    this,
                    LanguageVersionSettingsImpl.DEFAULT
                )
            }

            override fun builtInsForModule(module: ModuleInfo): KotlinBuiltIns = DefaultBuiltIns.Instance

            override fun sdkDependency(module: ModuleInfo): ModuleInfo? = null
        }
    }

    private fun createJvmResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        library: LibraryModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>,
        sourcesScope: GlobalSearchScope,
        builtIns: KotlinBuiltIns
    ): ResolverForProject<ModuleInfo> {
        val javaRoots = classpath
            .mapNotNull {
                val rootFile = when (it.extension) {
                    "jar" -> StandardFileSystems.jar().findFileByPath("${it.absolutePath}$JAR_SEPARATOR")
                    else -> StandardFileSystems.local().findFileByPath(it.absolutePath)
                }
                rootFile?.let { JavaRoot(it, JavaRoot.RootType.BINARY) }
            }

        return object : AbstractResolverForProject<ModuleInfo>(
            "Dokka",
            projectContext,
            modules = listOf(module, library)
        ) {
            override fun modulesContent(module: ModuleInfo): ModuleContent<ModuleInfo> =
                when (module) {
                    library -> ModuleContent(module, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                    module -> ModuleContent(module, emptyList(), sourcesScope)
                    else -> throw IllegalArgumentException("Unexpected module info")
                }

            override fun builtInsForModule(module: ModuleInfo): KotlinBuiltIns = builtIns

            override fun createResolverForModule(
                descriptor: ModuleDescriptor,
                moduleInfo: ModuleInfo
            ): ResolverForModule = JvmResolverForModuleFactory(
                JvmPlatformParameters(packagePartProviderFactory = { content ->
                    JvmPackagePartProvider(
                        configuration.languageVersionSettings,
                        content.moduleContentScope
                    )
                        .apply {
                            addRoots(javaRoots, messageCollector)
                        }
                }, moduleByJavaClass = {
                    val file =
                        (it as? BinaryJavaClass)?.virtualFile ?: (it as JavaClassImpl).psi.containingFile.virtualFile
                    if (file in sourcesScope)
                        module
                    else
                        library
                }, resolverForReferencedModule = null,
                    useBuiltinsProviderForModule = { false }),
                CompilerEnvironment,
                unspecifiedJvmPlatform
            ).createResolverForModule(
                descriptor as ModuleDescriptorImpl,
                projectContext.withModule(descriptor),
                modulesContent(moduleInfo),
                this,
                LanguageVersionSettingsImpl.DEFAULT
            )

            override fun sdkDependency(module: ModuleInfo): ModuleInfo? = null
        }
    }

    fun loadLanguageVersionSettings(languageVersionString: String?, apiVersionString: String?) {
        val languageVersion = LanguageVersion.fromVersionString(languageVersionString) ?: LanguageVersion.LATEST_STABLE
        val apiVersion =
            apiVersionString?.let { ApiVersion.parse(it) } ?: ApiVersion.createByLanguageVersion(languageVersion)
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(languageVersion, apiVersion)
    }

    /**
     * Classpath for this environment.
     */
    val classpath: List<File>
        get() = configuration.jvmClasspathRoots

    /**
     * Adds list of paths to classpath.
     * $paths: collection of files to add
     */
    fun addClasspath(paths: List<File>) {
        if (analysisPlatform == Platform.js) {
            configuration.addAll(JSConfigurationKeys.LIBRARIES, paths.map { it.absolutePath })
        }
        configuration.addJvmClasspathRoots(paths)
    }

    /**
     * Adds path to classpath.
     * $path: path to add
     */
    fun addClasspath(path: File) {
        if (analysisPlatform == Platform.js) {
            configuration.add(JSConfigurationKeys.LIBRARIES, path.absolutePath)
        }
        configuration.addJvmClasspathRoot(path)
    }

    /**
     * List of source roots for this environment.
     */
    val sources: List<String>
        get() = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<KotlinSourceRoot>()
            ?.map { it.path } ?: emptyList()

    /**
     * Adds list of paths to source roots.
     * $list: collection of files to add
     */
    fun addSources(sourceDirectories: Iterable<File>) {
        sourceDirectories.forEach { directory ->
            configuration.addKotlinSourceRoot(directory.path)
            if (directory.isDirectory || directory.extension == ".java") {
                configuration.addJavaSourceRoot(directory)
            }
        }
    }

    fun addRoots(list: List<ContentRoot>) {
        configuration.addAll(CLIConfigurationKeys.CONTENT_ROOTS, list)
    }

    /**
     * Disposes the environment and frees all associated resources.
     */
    override fun dispose() {
        Disposer.dispose(this)
    }

    companion object {
        private fun <T : Any> registerExtensionPoint(
            appExtension: ApplicationExtensionDescriptor<T>,
            instances: List<T>,
            disposable: Disposable
        ) {
            if (Extensions.getRootArea().hasExtensionPoint(appExtension.extensionPointName))
                return

            appExtension.registerExtensionPoint()
            instances.forEach { extension -> appExtension.registerExtension(extension, disposable) }
        }
    }
}


