package org.jetbrains.dokka

import com.google.common.collect.ImmutableMap
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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
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
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.container.tryGetService
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ide.konan.NativeLibraryInfo
import org.jetbrains.kotlin.ide.konan.analyzer.NativeResolverForModuleFactory
import org.jetbrains.kotlin.ide.konan.decompiler.KotlinNativeLoadingMetadataCache
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.JsResolverForModuleFactory
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.unspecifiedJvmPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.jvm.JvmResolverForModuleFactory
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.io.File

const val JAR_SEPARATOR = "!/"
const val KLIB_EXTENSION = "klib"

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
        System.setProperty("idea.io.use.fallback", "true")

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

        Extensions.registerAreaClass("IDEA_MODULE", null)
        CoreApplicationEnvironment.registerExtensionPoint(
            Extensions.getRootArea(),
            OrderEnumerationHandler.EP_NAME, OrderEnumerationHandler.Factory::class.java
        )

        projectComponentManager.registerService(
            ProjectFileIndex::class.java,
            projectFileIndex
        )

        projectComponentManager.registerService(
            ProjectRootManager::class.java,
            CoreProjectRootManager(projectFileIndex)
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
            Platform.native -> KonanPlatforms.defaultKonanPlatform
            Platform.jvm -> JvmPlatforms.defaultJvmPlatform
        }

        val nativeLibraries = classpath.filter { it.extension == KLIB_EXTENSION }
            .map { createNativeLibraryModuleInfo(it) }

        val library = object : LibraryModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<library>")
            override val platform: TargetPlatform = targetPlatform
            override fun dependencies(): List<ModuleInfo> = listOf(this)
            override fun getLibraryRoots(): Collection<String> =
                classpath.filterNot { it.extension == KLIB_EXTENSION }.map { it.absolutePath }
        }

        val module = object : ModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<module>")
            override val platform: TargetPlatform = targetPlatform
            override fun dependencies(): List<ModuleInfo> = listOf(this, library) + nativeLibraries
        }

        val sourcesScope = createSourceModuleSearchScope(environment.project, sourceFiles)
        val modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo> = {
            when (it) {
                library -> ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                module -> ModuleContent(it, emptyList(), GlobalSearchScope.allScope(environment.project))
                in nativeLibraries -> ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                else -> throw IllegalArgumentException("Unexpected module info")
            }
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
                library,
                modulesContent,
                environment
            )
            Platform.js -> createJsResolverForProject(projectContext, module, library, modulesContent)
            Platform.native -> createNativeResolverForProject(projectContext, module, library, modulesContent)

        }
        val libraryModuleDescriptor = resolverForProject.descriptorForModule(library)
        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        builtIns?.initialize(moduleDescriptor, true)

        val resolverForLibrary =
            resolverForProject.resolverForModule(library) // Required before module to initialize library properly
        val resolverForModule = resolverForProject.resolverForModule(module)
        val libraryResolutionFacade =
            DokkaResolutionFacade(environment.project, libraryModuleDescriptor, resolverForLibrary)
        val created = DokkaResolutionFacade(environment.project, moduleDescriptor, resolverForModule)
        val projectComponentManager = environment.project as MockComponentManager
        projectComponentManager.registerService(KotlinCacheService::class.java, CoreKotlinCacheService(created))

        return created to libraryResolutionFacade
    }

    private fun Platform.analyzerServices() = when (this) {
        Platform.js -> JsPlatformAnalyzerServices
        Platform.common -> CommonPlatformAnalyzerServices
        Platform.native -> NativePlatformAnalyzerServices
        Platform.jvm -> JvmPlatformAnalyzerServices
    }

    private fun createNativeLibraryModuleInfo(libraryFile: File): LibraryModuleInfo {
        val kotlinLibrary = createKotlinLibrary(org.jetbrains.kotlin.konan.file.File(libraryFile.absolutePath), false)
        return object : LibraryModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<klib>")
            override val platform: TargetPlatform = KonanPlatforms.defaultKonanPlatform
            override fun dependencies(): List<ModuleInfo> = listOf(this)
            override fun getLibraryRoots(): Collection<String> = listOf(libraryFile.absolutePath)
            override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
                get() = super.capabilities + (NativeLibraryInfo.NATIVE_LIBRARY_CAPABILITY to kotlinLibrary)
        }
    }

    private fun createCommonResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        library: LibraryModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>,
        environment: KotlinCoreEnvironment
    ): ResolverForProject<ModuleInfo> {
        return object : AbstractResolverForProject<ModuleInfo>(
            "Dokka",
            projectContext,
            modules = listOf(module, library)
        ) {
            override fun sdkDependency(module: ModuleInfo): ModuleInfo? = null

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
        }
    }

    private fun createJsResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        library: LibraryModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>
    ): ResolverForProject<ModuleInfo> {
        return object : AbstractResolverForProject<ModuleInfo>(
            "Dokka",
            projectContext,
            modules = listOf(module, library)
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
        library: LibraryModuleInfo,
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
                (ApplicationManager.getApplication() as MockApplication).addComponent(
                    KotlinNativeLoadingMetadataCache::class.java,
                    KotlinNativeLoadingMetadataCache()
                )

                return NativeResolverForModuleFactory(
                    PlatformAnalysisParameters.Empty,
                    CompilerEnvironment,
                    KonanPlatforms.defaultKonanPlatform
                ).createResolverForModule(
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
                    "jar" -> StandardFileSystems.jar().findFileByPath("${it.absolutePath}${JAR_SEPARATOR}")
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
                JvmPlatformParameters({ content ->
                    JvmPackagePartProvider(
                        configuration.languageVersionSettings,
                        content.moduleContentScope
                    )
                        .apply {
                            addRoots(javaRoots, messageCollector)
                        }
                }, {
                    val file = (it as JavaClassImpl).psi.containingFile.virtualFile
                    if (file in sourcesScope)
                        module
                    else
                        library
                }),
                CompilerEnvironment,
                KonanPlatforms.defaultKonanPlatform
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
    fun addSources(list: List<String>) {
        list.forEach {
            configuration.addKotlinSourceRoot(it)
            val file = File(it)
            if (file.isDirectory || file.extension == ".java") {
                configuration.addJavaSourceRoot(file)
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
}

fun contentRootFromPath(path: String): ContentRoot {
    val file = File(path)
    return if (file.extension == "java") JavaSourceRoot(file, null) else KotlinSourceRoot(path, false)
}


class DokkaResolutionFacade(
    override val project: Project,
    override val moduleDescriptor: ModuleDescriptor,
    val resolverForModule: ResolverForModule
) : ResolutionFacade {
    override fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>): AnalysisResult {
        throw UnsupportedOperationException()
    }

    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        return resolverForModule.componentProvider.tryGetService(serviceClass)
    }

    override fun resolveToDescriptor(
        declaration: KtDeclaration,
        bodyResolveMode: BodyResolveMode
    ): DeclarationDescriptor {
        return resolveSession.resolveToDescriptor(declaration)
    }

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        throw UnsupportedOperationException()
    }

    val resolveSession: ResolveSession get() = getFrontendService(ResolveSession::class.java)

    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        if (element is KtDeclaration) {
            val descriptor = resolveToDescriptor(element)
            return object : BindingContext {
                override fun <K : Any?, V : Any?> getKeys(p0: WritableSlice<K, V>?): Collection<K> {
                    throw UnsupportedOperationException()
                }

                override fun getType(p0: KtExpression): KotlinType? {
                    throw UnsupportedOperationException()
                }

                override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>?, key: K): V? {
                    if (key != element) {
                        throw UnsupportedOperationException()
                    }
                    return when {
                        slice == BindingContext.DECLARATION_TO_DESCRIPTOR -> descriptor as V
                        slice == BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER && (element as KtParameter).hasValOrVar() -> descriptor as V
                        else -> null
                    }
                }

                override fun getDiagnostics(): Diagnostics {
                    throw UnsupportedOperationException()
                }

                override fun addOwnDataTo(p0: BindingTrace, p1: Boolean) {
                    throw UnsupportedOperationException()
                }

                override fun <K : Any?, V : Any?> getSliceContents(p0: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
                    throw UnsupportedOperationException()
                }

            }
        }
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        return resolverForModule.componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return resolverForModule.componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

}
