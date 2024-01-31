/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.mock.MockComponentManager
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.impl.PsiNameHelperImpl
import com.intellij.psi.impl.source.javadoc.JavadocManagerImpl
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocManager
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.AnalysisContextCreator
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KLibService
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.MockApplicationHack
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
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
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms.unspecifiedJvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CliSealedClassInheritorsProvider
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.jvm.JvmResolverForModuleFactory
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

internal const val JAR_SEPARATOR = "!/"

/**
 * Kotlin as a service entry point
 *
 * Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode
 *
 * $messageCollector: required by compiler infrastructure and will receive all compiler messages
 * $body: optional and can be used to configure environment without creating local variable
 */
@InternalDokkaApi
public class AnalysisEnvironment(
    private val messageCollector: MessageCollector,
    internal val analysisPlatform: Platform,
    private val mockApplicationHack: MockApplicationHack,
    private val kLibService: KLibService,
) : Disposable {
    private val configuration = CompilerConfiguration()

    init {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    }

    internal fun createCoreEnvironment(): KotlinCoreEnvironment {
        System.setProperty("idea.io.use.nio2", "true")
        System.setProperty("idea.ignore.disabled.plugins", "true")

        val configFiles = when (analysisPlatform) {
            Platform.jvm, Platform.common -> EnvironmentConfigFiles.JVM_CONFIG_FILES
            Platform.native -> EnvironmentConfigFiles.NATIVE_CONFIG_FILES
            Platform.js, Platform.wasm -> EnvironmentConfigFiles.JS_CONFIG_FILES
        }

        val environment = KotlinCoreEnvironment.createForProduction(this, configuration, configFiles)
        val projectComponentManager = environment.project as MockComponentManager

        CoreApplicationEnvironment.registerExtensionPoint(
            environment.project.extensionArea,
            JavadocTagInfo.EP_NAME, JavadocTagInfo::class.java
        )

        @Suppress("DEPRECATION")
        val extensionArea = Extensions.getRootArea()

        CoreApplicationEnvironment.registerExtensionPoint(
            extensionArea,
            CustomJavadocTagProvider.EP_NAME, CustomJavadocTagProvider::class.java
        )

        // TODO: figure out why compilation fails with unresolved `CoreApplicationEnvironment.registerApplicationService(...)`
        //  call, fix it appropriately
        with(ApplicationManager.getApplication() as MockApplication) {
            mockApplicationHack.hack(this)
        }

        projectComponentManager.registerService(
            JavadocManager::class.java,
            JavadocManagerImpl(environment.project)
        )

        projectComponentManager.registerService(
            PsiNameHelper::class.java,
            PsiNameHelperImpl(environment.project)
        )

        projectComponentManager.registerService(
            CustomJavadocTagProvider::class.java,
            CustomJavadocTagProvider { emptyList() }
        )

        return environment
    }

    private fun createSourceModuleSearchScope(project: Project, sourceFiles: List<KtFile>): GlobalSearchScope =
        when (analysisPlatform) {
            Platform.jvm -> TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            Platform.js, Platform.common, Platform.native, Platform.wasm -> GlobalSearchScope.filesScope(
                project,
                sourceFiles.map { it.virtualFile }.toSet()
            )
        }

    internal fun createResolutionFacade(
        environment: KotlinCoreEnvironment,
        analysisContextCreator: AnalysisContextCreator,
        ignoreCommonBuiltIns: Boolean = false
    ): AnalysisContext {
        val projectContext = ProjectContext(environment.project, "Dokka")
        val sourceFiles = environment.getSourceFiles()

        val targetPlatform = when (analysisPlatform) {
            Platform.js, Platform.wasm -> JsPlatforms.defaultJsPlatform
            Platform.common -> CommonPlatforms.defaultCommonPlatform
            Platform.native -> NativePlatforms.unspecifiedNativePlatform
            Platform.jvm -> JvmPlatforms.defaultJvmPlatform
        }

        val kotlinLibraries: Map<AbsolutePathString, KotlinLibrary> = resolveKotlinLibraries()

        val commonDependencyContainer = if (analysisPlatform == Platform.common) DokkaKlibMetadataCommonDependencyContainer(
            kotlinLibraries.values.toList(),
            environment.configuration,
            LockBasedStorageManager("DokkaKlibMetadata")
        ) else null

        val extraModuleDependencies = kotlinLibraries.values.registerLibraries() + commonDependencyContainer?.moduleInfos.orEmpty()

        val library = object : LibraryModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<library>")
            override val platform: TargetPlatform = targetPlatform
            override fun dependencies(): List<ModuleInfo> = listOf(this)
            override fun getLibraryRoots(): Collection<String> = classpath
                .map { libraryFile -> libraryFile.absolutePath }
                .filter { path -> path !in kotlinLibraries }
        }

        val module = object : ModuleInfo {
            override val analyzerServices: PlatformDependentAnalyzerServices =
                analysisPlatform.analyzerServices()
            override val name: Name = Name.special("<module>")
            override val platform: TargetPlatform = targetPlatform
            override fun dependencies(): List<ModuleInfo> =
                listOf(this, library) + extraModuleDependencies

            /**
             * Only for common platform ignore BuiltIns for StdLib since it can cause a conflict
             * between BuiltIns from a compiler and ones from source code.
             */
            override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns {
                return if (analysisPlatform == Platform.common && ignoreCommonBuiltIns) ModuleInfo.DependencyOnBuiltIns.NONE
                else super.dependencyOnBuiltIns()
            }
        }

        val sourcesScope = createSourceModuleSearchScope(environment.project, sourceFiles)
        val modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo> = {
            when (it) {
                library -> ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                module -> ModuleContent(it, emptyList(), GlobalSearchScope.allScope(environment.project))
                is DokkaKlibLibraryInfo -> {
                    if (it.libraryRoot in kotlinLibraries)
                        ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
                    else null
                }
                is CommonKlibModuleInfo -> ModuleContent(it, emptyList(), GlobalSearchScope.notScope(sourcesScope))
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
                environment,
                commonDependencyContainer
            )
            Platform.js, Platform.wasm -> createJsResolverForProject(projectContext, module, modulesContent)
            Platform.native -> createNativeResolverForProject(projectContext, module, modulesContent)

        }
        @Suppress("UNUSED_VARIABLE") // BEWARE!!!! IT's UNUSED, but without it some things don't work
        val libraryModuleDescriptor = resolverForProject.descriptorForModule(library)

        val moduleDescriptor = resolverForProject.descriptorForModule(module)
        builtIns?.initialize(moduleDescriptor, true)

        @Suppress("UNUSED_VARIABLE") // BEWARE!!!! IT's UNUSED, but without it some things don't work
        val resolverForLibrary = resolverForProject.resolverForModule(library) // Required before module to initialize library properly

        val resolverForModule = resolverForProject.resolverForModule(module)

        return analysisContextCreator.create(
            environment.project as MockProject,
            moduleDescriptor,
            resolverForModule,
            environment,
            this
        )
    }

    private fun Platform.analyzerServices() = when (this) {
        Platform.js, Platform.wasm -> JsPlatformAnalyzerServices
        Platform.common -> CommonPlatformAnalyzerServices
        Platform.native -> NativePlatformAnalyzerServices
        Platform.jvm -> JvmPlatformAnalyzerServices
    }

    private fun Collection<KotlinLibrary>.registerLibraries(): List<DokkaKlibLibraryInfo> {
        if (analysisPlatform != Platform.native && analysisPlatform != Platform.js && analysisPlatform != Platform.wasm) return emptyList()
        val dependencyResolver = DokkaKlibLibraryDependencyResolver()
        val analyzerServices = analysisPlatform.analyzerServices()

        return map { kotlinLibrary ->
            if (analysisPlatform == org.jetbrains.dokka.Platform.native) DokkaNativeKlibLibraryInfo(
                kotlinLibrary,
                analyzerServices,
                dependencyResolver
            )
            else DokkaJsKlibLibraryInfo(kotlinLibrary, analyzerServices, dependencyResolver)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun resolveKotlinLibraries(): Map<AbsolutePathString, KotlinLibrary> {
        return if (analysisPlatform == Platform.jvm) emptyMap() else buildMap {
            classpath
                .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
                .forEach { libraryFile ->
                    try {
                        val kotlinLibrary = resolveSingleFileKlib(
                            libraryFile = KFile(libraryFile.absolutePath),
                            strategy = ToolingSingleFileKlibResolveStrategy
                        )
                        if (kLibService.isAnalysisCompatible(kotlinLibrary)) {
                            // exists, is KLIB, has compatible format
                            put(
                                libraryFile.absolutePath,
                                kotlinLibrary
                            )
                        }
                    } catch (e: Throwable) {
                        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                            .report(CompilerMessageSeverity.WARNING, "Can not resolve KLIB. " + e.message)
                    }
                }
        }
    }

    private fun createCommonResolverForProject(
        projectContext: ProjectContext,
        module: ModuleInfo,
        modulesContent: (ModuleInfo) -> ModuleContent<ModuleInfo>,
        environment: KotlinCoreEnvironment,
        dependencyContainer: CommonDependenciesContainer?
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
                                            CommonAnalysisParameters(
                                                metadataPartProviderFactory = { content ->
                                                    environment.createPackagePartProvider(content.moduleContentScope)
                                                }
                                            ),
                                            CompilerEnvironment,
                                            unspecifiedJvmPlatform,
                                            true,
                                            dependencyContainer
                                        ).createResolverForModule(
                    moduleDescriptor = descriptor as ModuleDescriptorImpl,
                    moduleContext = projectContext.withModule(descriptor),
                    moduleContent = modulesContent(moduleInfo),
                    resolverForProject = this,
                    languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                    sealedInheritorsProvider = CliSealedClassInheritorsProvider,
                    resolveOptimizingOptions = null,
                    absentDescriptorHandlerClass = null
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
            ): ResolverForModule = DokkaJsResolverForModuleFactory(CompilerEnvironment, kLibService).createResolverForModule(
                moduleDescriptor = descriptor as ModuleDescriptorImpl,
                moduleContext = projectContext.withModule(descriptor),
                moduleContent = modulesContent(moduleInfo),
                resolverForProject = this,
                languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                sealedInheritorsProvider = CliSealedClassInheritorsProvider,
                resolveOptimizingOptions = null,
                absentDescriptorHandlerClass = null
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

                return DokkaNativeResolverForModuleFactory(CompilerEnvironment, kLibService).createResolverForModule(
                    moduleDescriptor = descriptor as ModuleDescriptorImpl,
                    moduleContext = projectContext.withModule(descriptor),
                    moduleContent = modulesContent(moduleInfo),
                    resolverForProject = this,
                    languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                    sealedInheritorsProvider = CliSealedClassInheritorsProvider,
                    resolveOptimizingOptions = null,
                    absentDescriptorHandlerClass = null
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
            .mapNotNull { file ->
                val rootFile = when (file.extension) {
                    "jar" -> StandardFileSystems.jar().findFileByPath("${file.absolutePath}$JAR_SEPARATOR")
                    else -> StandardFileSystems.local().findFileByPath(file.absolutePath)
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
                moduleDescriptor = descriptor as ModuleDescriptorImpl,
                moduleContext = projectContext.withModule(descriptor),
                moduleContent = modulesContent(moduleInfo),
                resolverForProject = this,
                languageVersionSettings = configuration.languageVersionSettings,
                sealedInheritorsProvider = CliSealedClassInheritorsProvider,
                resolveOptimizingOptions = null,
                absentDescriptorHandlerClass = null
            )

            override fun sdkDependency(module: ModuleInfo): ModuleInfo? = null
        }
    }

    internal fun loadLanguageVersionSettings(languageVersionString: String?, apiVersionString: String?) {
        val languageVersion = LanguageVersion.fromVersionString(languageVersionString) ?: LanguageVersion.LATEST_STABLE
        val apiVersion =
            apiVersionString?.let { ApiVersion.parse(it) } ?: ApiVersion.createByLanguageVersion(languageVersion)
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = languageVersion,
            apiVersion = apiVersion, analysisFlags = hashMapOf(
                // force to resolve light classes (lazily by default)
                AnalysisFlags.eagerResolveOfLightClasses to true
            )
        )
    }

    /**
     * Classpath for this environment.
     */
    private val classpath: List<File>
        get() = configuration.jvmClasspathRoots + configuration.getList(JSConfigurationKeys.LIBRARIES)
            .mapNotNull { File(it) }

    /**
     * Adds list of paths to classpath.
     * $paths: collection of files to add
     */
    internal fun addClasspath(paths: List<File>) {
        if (analysisPlatform == Platform.js || analysisPlatform == Platform.wasm) {
            configuration.addAll(JSConfigurationKeys.LIBRARIES, paths.map { it.absolutePath })
        } else {
            configuration.addJvmClasspathRoots(paths)
        }
    }

    // Set up JDK classpath roots explicitly because of https://github.com/JetBrains/kotlin/commit/f89765eb33dd95c8de33a919cca83651b326b246
    internal fun configureJdkClasspathRoots() {
        val jdkHome = File(System.getProperty("java.home"))
        if (!jdkHome.exists()) {
            messageCollector.report(CompilerMessageSeverity.WARNING, "Set existed java.home to use JDK")
        }
        configuration.put(JVMConfigurationKeys.JDK_HOME, jdkHome)
        configuration.configureJdkClasspathRoots() // only non-nodular JDK
    }
    /**
     * Adds path to classpath.
     * $path: path to add
     */
    internal fun addClasspath(path: File) {
        if (analysisPlatform == Platform.js || analysisPlatform == Platform.wasm) {
            configuration.add(JSConfigurationKeys.LIBRARIES, path.absolutePath)
        } else {
            configuration.addJvmClasspathRoot(path)
        }
    }

    /**
     * List of source roots for this environment.
     */
    internal val sources: List<String>
        get() = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<KotlinSourceRoot>()
            ?.map { it.path } ?: emptyList()

    /**
     * Adds list of paths to source roots.
     * $list: collection of files to add
     */
    internal fun addSources(sourceDirectories: Iterable<File>) {
        sourceDirectories.forEach { directory ->
            configuration.addKotlinSourceRoot(directory.path)
            if (directory.isDirectory || directory.extension == "java") {
                configuration.addJavaSourceRoot(directory)
            }
        }
    }

    internal fun addRoots(list: List<ContentRoot>) {
        configuration.addAll(CLIConfigurationKeys.CONTENT_ROOTS, list)
    }

    /**
     * Disposes the environment and frees all associated resources.
     */
    override fun dispose() {
        Disposer.dispose(this)
    }

}


