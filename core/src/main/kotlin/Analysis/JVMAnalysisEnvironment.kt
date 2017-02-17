package org.jetbrains.dokka

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreModuleManager
import com.intellij.mock.MockComponentManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerationHandler
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.storage.StorageManager

class JVMAnalysisEnvironment(messageCollector: MessageCollector) : AnalysisEnvironment(messageCollector) {

    lateinit var builtIns: JvmBuiltIns

    override fun createBuiltIns(storageManager: StorageManager): (ModuleInfo) -> KotlinBuiltIns {
        builtIns = JvmBuiltIns(storageManager, true)
        return { builtIns }
    }

    override fun initializeBuiltIns(moduleDescriptor: ModuleDescriptor) {
        builtIns.initialize(moduleDescriptor, true)
    }

    override fun <M : ModuleInfo> setupResolverForProject(projectContext: ProjectContext,
                                                          library: M, module: M,
                                                          moduleContent: (ModuleInfo) -> ModuleContent,
                                                          builtIns: (ModuleInfo) -> KotlinBuiltIns,
                                                          environment: KotlinCoreEnvironment,
                                                          sourcesScope: GlobalSearchScope): ResolverForProject<M> {
        return JvmAnalyzerFacade.setupResolverForProject(
                "Dokka",
                projectContext,
                listOf(library, module),
                moduleContent,
                JvmPlatformParameters {
                    val file = (it as JavaClassImpl).psi.containingFile.virtualFile
                    if (file in sourcesScope)
                        module
                    else
                        library
                },
                CompilerEnvironment,
                packagePartProviderFactory = {
                    info, content ->
                    JvmPackagePartProvider(environment, content.moduleContentScope)
                },
                builtIns = builtIns,
                modulePlatforms = { MultiTargetPlatform.Specific("JVM") }
        )
    }

    override fun createCoreEnvironment(): KotlinCoreEnvironment {
        System.setProperty("idea.io.use.fallback", "true")
        val environment = KotlinCoreEnvironment.createForProduction(this, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val projectComponentManager = environment.project as MockComponentManager

        val projectFileIndex = CoreProjectFileIndex(environment.project,
                environment.configuration.getList(JVMConfigurationKeys.CONTENT_ROOTS))

        val moduleManager = object : CoreModuleManager(environment.project, this) {
            override fun getModules(): Array<out Module> = arrayOf(projectFileIndex.module)
        }

        CoreApplicationEnvironment.registerComponentInstance(projectComponentManager.picoContainer,
                ModuleManager::class.java, moduleManager)

        Extensions.registerAreaClass("IDEA_MODULE", null)
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(),
                OrderEnumerationHandler.EP_NAME, OrderEnumerationHandler.Factory::class.java)

        projectComponentManager.registerService(ProjectFileIndex::class.java,
                projectFileIndex)
        projectComponentManager.registerService(ProjectRootManager::class.java,
                CoreProjectRootManager(projectFileIndex))
        return environment
    }

    override fun createSourceModuleSearchScope(project: Project, sourceFiles: List<KtFile>): GlobalSearchScope {
        return TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
    }
}

