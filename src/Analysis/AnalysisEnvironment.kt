package org.jetbrains.dokka

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreModuleManager
import com.intellij.mock.MockComponentManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForModule
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.caches.resolve.KotlinOutOfBlockCompletionModificationTracker
import org.jetbrains.kotlin.idea.caches.resolve.LibraryModificationTracker
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.io.File

/**
 * Kotlin as a service entry point
 *
 * Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode
 *
 * $messageCollector: required by compiler infrastructure and will receive all compiler messages
 * $body: optional and can be used to configure environment without creating local variable
 */
public class AnalysisEnvironment(val messageCollector: MessageCollector, body: AnalysisEnvironment.() -> Unit = {}) : Disposable {
    val configuration = CompilerConfiguration();

    init {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        body()
    }

    /**
     * Executes [processor] when analysis is complete.
     * $processor: function to receive compiler environment, module and context for symbol resolution
     */
    public fun <T> withContext(processor: (KotlinCoreEnvironment, ResolutionFacade, ResolveSession) -> T): T {
        val environment = KotlinCoreEnvironment.createForProduction(this, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val projectComponentManager = environment.project as MockComponentManager

        val moduleManager = CoreModuleManager(environment.project, this)
        CoreApplicationEnvironment.registerComponentInstance(projectComponentManager.picoContainer,
                ModuleManager::class.java, moduleManager)

        projectComponentManager.registerService(ProjectFileIndex::class.java,
                CoreProjectFileIndex())
        projectComponentManager.registerService(LibraryModificationTracker::class.java,
                LibraryModificationTracker(environment.project))
        projectComponentManager.registerService(KotlinCacheService::class.java,
                KotlinCacheService(environment.project))
        projectComponentManager.registerService(KotlinOutOfBlockCompletionModificationTracker::class.java,
                KotlinOutOfBlockCompletionModificationTracker())

        val projectContext = ProjectContext(environment.project)
        val sourceFiles = environment.getSourceFiles()

        val module = object : ModuleInfo {
            override val name: Name = Name.special("<module>")
            override fun dependencies(): List<ModuleInfo> = listOf(this)
        }
        val resolverForProject = JvmAnalyzerFacade.setupResolverForProject(
                "Dokka",
                projectContext,
                listOf(module),
                { ModuleContent(sourceFiles, GlobalSearchScope.allScope(environment.project)) },
                JvmPlatformParameters { module },
                CompilerEnvironment
        )

        val resolverForModule = resolverForProject.resolverForModule(module)
        val resolveSession = resolverForModule.componentProvider.get<ResolveSession>()
        val facade = DokkaResolutionFacade(environment.project, resolverForProject.descriptorForModule(module), resolverForModule)
        return processor(environment, facade, resolveSession)
    }

    /**
     * Classpath for this environment.
     */
    public val classpath: List<File>
        get() = configuration.jvmClasspathRoots

    /**
     * Adds list of paths to classpath.
     * $paths: collection of files to add
     */
    public fun addClasspath(paths: List<File>) {
        configuration.addJvmClasspathRoots(paths)
    }

    /**
     * Adds path to classpath.
     * $path: path to add
     */
    public fun addClasspath(path: File) {
        configuration.addJvmClasspathRoot(path)
    }

    /**
     * List of source roots for this environment.
     */
    public val sources: List<String>
        get() = configuration.get(CommonConfigurationKeys.CONTENT_ROOTS)
                ?.filterIsInstance<KotlinSourceRoot>()
                ?.map { it.path } ?: emptyList()

    /**
     * Adds list of paths to source roots.
     * $list: collection of files to add
     */
    public fun addSources(list: List<String>) {
        list.forEach {
            configuration.add(CommonConfigurationKeys.CONTENT_ROOTS, contentRootFromPath(it))
        }
    }

    public fun addRoots(list: List<ContentRoot>) {
        configuration.addAll(CommonConfigurationKeys.CONTENT_ROOTS, list)
    }

    /**
     * Disposes the environment and frees all associated resources.
     */
    public override fun dispose() {
        Disposer.dispose(this)
    }
}

public fun contentRootFromPath(path: String): ContentRoot {
    val file = File(path)
    return if (file.extension == "java") JavaSourceRoot(file) else KotlinSourceRoot(path)
}


class DokkaResolutionFacade(override val project: Project,
                            override val moduleDescriptor: ModuleDescriptor,
                            val resolverForModule: ResolverForModule) : ResolutionFacade {

    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        throw UnsupportedOperationException()
    }

    override fun analyzeFullyAndGetResult(elements: Collection<KtElement>): AnalysisResult {
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        return resolverForModule.componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor {
        throw UnsupportedOperationException()
    }
}
