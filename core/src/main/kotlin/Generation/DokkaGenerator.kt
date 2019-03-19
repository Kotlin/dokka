package org.jetbrains.dokka

import com.google.inject.Guice
import com.google.inject.Injector
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.dokka.Generation.DocumentationMerger
import org.jetbrains.dokka.Utilities.DokkaAnalysisModule
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.dokka.Utilities.DokkaRunModule
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.system.measureTimeMillis

class DokkaGenerator(val dokkaConfiguration: DokkaConfiguration,
                     val logger: DokkaLogger) {

    private val documentationModules: MutableList<DocumentationModule> = mutableListOf()
    private val globalInjector = Guice.createInjector(DokkaRunModule(dokkaConfiguration))


    fun generate() = with(dokkaConfiguration) {


        for (pass in passesConfigurations) {
            val documentationModule = DocumentationModule(pass.moduleName)
            appendSourceModule(pass, documentationModule)
            documentationModules.add(documentationModule)
        }

        val totalDocumentationModule = DocumentationMerger(documentationModules, logger).merge()
        totalDocumentationModule.prepareForGeneration(dokkaConfiguration)

        val timeBuild = measureTimeMillis {
            logger.info("Generating pages... ")
            val outputInjector = globalInjector.createChildInjector(DokkaOutputModule(dokkaConfiguration, logger))
            val instance = outputInjector.getInstance(Generator::class.java)
            instance.buildAll(totalDocumentationModule)
        }
        logger.info("done in ${timeBuild / 1000} secs")
    }

    private fun appendSourceModule(
        passConfiguration: DokkaConfiguration.PassConfiguration,
        documentationModule: DocumentationModule
    ) = with(passConfiguration) {

        val sourcePaths = passConfiguration.sourceRoots.map { it.path }
        val environment = createAnalysisEnvironment(sourcePaths, passConfiguration)

        logger.info("Module: $moduleName")
        logger.info("Output: ${File(dokkaConfiguration.outputDir)}")
        logger.info("Sources: ${sourcePaths.joinToString()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val defaultPlatformAsList = passConfiguration.targets
        val defaultPlatformsProvider = object : DefaultPlatformsProvider {
            override fun getDefaultPlatforms(descriptor: DeclarationDescriptor): List<String> {
//                val containingFilePath = descriptor.sourcePsi()?.containingFile?.virtualFile?.canonicalPath
//                        ?.let { File(it).absolutePath }
//                val sourceRoot = containingFilePath?.let { path -> sourceRoots.find { path.startsWith(it.path) } }
                if (descriptor is MemberDescriptor && descriptor.isExpect) {
                    return defaultPlatformAsList.take(1)
                }
                return /*sourceRoot?.platforms ?: */defaultPlatformAsList
            }
        }

        val injector = globalInjector.createChildInjector(
                DokkaAnalysisModule(environment, dokkaConfiguration, defaultPlatformsProvider, documentationModule.nodeRefGraph, passConfiguration, logger))

        buildDocumentationModule(injector, documentationModule, { isNotSample(it, passConfiguration.samples) }, includes)

        val timeAnalyse = System.currentTimeMillis() - startAnalyse
        logger.info("done in ${timeAnalyse / 1000} secs")

        Disposer.dispose(environment)
    }

    fun createAnalysisEnvironment(
        sourcePaths: List<String>,
        passConfiguration: DokkaConfiguration.PassConfiguration
    ): AnalysisEnvironment {
        val environment = AnalysisEnvironment(DokkaMessageCollector(logger), passConfiguration.analysisPlatform)

        environment.apply {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            //   addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
            for (element in passConfiguration.classpath) {
                addClasspath(File(element))
            }

            addSources(sourcePaths)
            addSources(passConfiguration.samples)

            loadLanguageVersionSettings(passConfiguration.languageVersion, passConfiguration.apiVersion)
        }

        return environment
    }

   private fun isNotSample(file: PsiFile, samples: List<String>): Boolean {
        val sourceFile = File(file.virtualFile!!.path)
        return samples.none { sample ->
            val canonicalSample = File(sample).canonicalPath
            val canonicalSource = sourceFile.canonicalPath
            canonicalSource.startsWith(canonicalSample)
        }
    }
}

class DokkaMessageCollector(val logger: DokkaLogger) : MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

fun buildDocumentationModule(injector: Injector,
                             documentationModule: DocumentationModule,
                             filesToDocumentFilter: (PsiFile) -> Boolean = { file -> true },
                             includes: List<String> = listOf()) {

    val coreEnvironment = injector.getInstance(KotlinCoreEnvironment::class.java)
    val fragmentFiles = coreEnvironment.getSourceFiles().filter(filesToDocumentFilter)

    val resolutionFacade = injector.getInstance(DokkaResolutionFacade::class.java)
    val analyzer = resolutionFacade.getFrontendService(LazyTopDownAnalyzer::class.java)
    analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, fragmentFiles)

    val fragments = fragmentFiles.mapNotNull { resolutionFacade.resolveSession.getPackageFragment(it.packageFqName) }
            .distinct()

    val packageDocs = injector.getInstance(PackageDocs::class.java)
    for (include in includes) {
        packageDocs.parse(include, fragments)
    }
    if (documentationModule.content.isEmpty()) {
        documentationModule.updateContent {
            for (node in packageDocs.moduleContent.children) {
                append(node)
            }
        }
    }

    parseJavaPackageDocs(packageDocs, coreEnvironment)

    with(injector.getInstance(DocumentationBuilder::class.java)) {
        documentationModule.appendFragments(fragments, packageDocs.packageContent,
                injector.getInstance(PackageDocumentationBuilder::class.java))

        propagateExtensionFunctionsToSubclasses(fragments, resolutionFacade)
    }

    val javaFiles = coreEnvironment.getJavaSourceFiles().filter(filesToDocumentFilter)
    with(injector.getInstance(JavaDocumentationBuilder::class.java)) {
        javaFiles.map { appendFile(it, documentationModule, packageDocs.packageContent) }
    }
}

fun parseJavaPackageDocs(packageDocs: PackageDocs, coreEnvironment: KotlinCoreEnvironment) {
    val contentRoots = coreEnvironment.configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.map { it.file }
            ?: listOf()
    contentRoots.forEach { root ->
        root.walkTopDown().filter { it.name == "overview.html" }.forEach {
            packageDocs.parseJava(it.path, it.relativeTo(root).parent.replace("/", "."))
        }
    }
}


fun KotlinCoreEnvironment.getJavaSourceFiles(): List<PsiJavaFile> {
    val sourceRoots = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.map { it.file }
            ?: listOf()

    val result = arrayListOf<PsiJavaFile>()
    val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")
    sourceRoots.forEach { sourceRoot ->
        sourceRoot.absoluteFile.walkTopDown().forEach {
            val vFile = localFileSystem.findFileByPath(it.path)
            if (vFile != null) {
                val psiFile = PsiManager.getInstance(project).findFile(vFile)
                if (psiFile is PsiJavaFile) {
                    result.add(psiFile)
                }
            }
        }
    }
    return result
}