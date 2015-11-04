package org.jetbrains.dokka

import com.google.inject.Guice
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.dokka.Utilities.GuiceModule
import org.jetbrains.kotlin.cli.common.arguments.ValueDescription
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.util.measureTimeMillis

class DokkaArguments {
    @set:Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    @ValueDescription("<path>")
    public var src: String = ""

    @set:Argument(value = "srcLink", description = "Mapping between a source directory and a Web site for browsing the code")
    @ValueDescription("<path>=<url>[#lineSuffix]")
    public var srcLink: String = ""

    @set:Argument(value = "include", description = "Markdown files to load (allows many paths separated by the system path separator)")
    @ValueDescription("<path>")
    public var include: String = ""

    @set:Argument(value = "samples", description = "Source root for samples")
    @ValueDescription("<path>")
    public var samples: String = ""

    @set:Argument(value = "output", description = "Output directory path")
    @ValueDescription("<path>")
    public var outputDir: String = "out/doc/"

    @set:Argument(value = "format", description = "Output format (text, html, markdown, jekyll, kotlin-website)")
    @ValueDescription("<name>")
    public var outputFormat: String = "html"

    @set:Argument(value = "module", description = "Name of the documentation module")
    @ValueDescription("<name>")
    public var moduleName: String = ""

    @set:Argument(value = "classpath", description = "Classpath for symbol resolution")
    @ValueDescription("<path>")
    public var classpath: String = ""

    @set:Argument(value = "nodeprecated", description = "Exclude deprecated members from documentation")
    public var nodeprecated: Boolean = false

}

private fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinition {
    val (path, urlAndLine) = srcLink.split('=')
    return SourceLinkDefinition(File(path).absolutePath,
            urlAndLine.substringBefore("#"),
            urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#" + it })
}

public fun main(args: Array<String>) {
    val arguments = DokkaArguments()
    val freeArgs: List<String> = Args.parse(arguments, args) ?: listOf()
    val sources = if (arguments.src.isNotEmpty()) arguments.src.split(File.pathSeparatorChar).toList() + freeArgs else freeArgs
    val samples = if (arguments.samples.isNotEmpty()) arguments.samples.split(File.pathSeparatorChar).toList() else listOf()
    val includes = if (arguments.include.isNotEmpty()) arguments.include.split(File.pathSeparatorChar).toList() else listOf()

    val sourceLinks = if (arguments.srcLink.isNotEmpty() && arguments.srcLink.contains("="))
        listOf(parseSourceLinkDefinition(arguments.srcLink))
    else {
        if (arguments.srcLink.isNotEmpty()) {
            println("Warning: Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
        }
        listOf()
    }

    val classPath = arguments.classpath.split(File.pathSeparatorChar).toList()
    val generator = DokkaGenerator(
            DokkaConsoleLogger,
            classPath,
            sources,
            samples,
            includes,
            arguments.moduleName,
            arguments.outputDir,
            arguments.outputFormat,
            sourceLinks,
            arguments.nodeprecated)

    generator.generate()
    DokkaConsoleLogger.report()
}

interface DokkaLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
}

object DokkaConsoleLogger: DokkaLogger {
    var warningCount: Int = 0

    override fun info(message: String) = println(message)
    override fun warn(message: String) {
        println("WARN: $message")
        warningCount++
    }

    override fun error(message: String) = println("ERROR: $message")

    fun report() {
        if (warningCount > 0) {
            println("Generation completed with $warningCount warnings")
        } else {
            println("Generation completed successfully")
        }
    }
}

class DokkaMessageCollector(val logger: DokkaLogger): MessageCollector {
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }
}

class DokkaGenerator(val logger: DokkaLogger,
                     val classpath: List<String>,
                     val sources: List<String>,
                     val samples: List<String>,
                     val includes: List<String>,
                     val moduleName: String,
                     val outputDir: String,
                     val outputFormat: String,
                     val sourceLinks: List<SourceLinkDefinition>,
                     val skipDeprecated: Boolean = false) {
    fun generate() {
        val environment = createAnalysisEnvironment()

        logger.info("Module: $moduleName")
        logger.info("Output: ${File(outputDir).absolutePath}")
        logger.info("Sources: ${environment.sources.joinToString()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val options = DocumentationOptions(false, sourceLinks = sourceLinks, skipDeprecated = skipDeprecated)

        val injector = Guice.createInjector(GuiceModule(this))
        val generator = injector.getInstance(Generator::class.java)

        val packageDocumentationBuilder = injector.getInstance(PackageDocumentationBuilder::class.java)

        val documentation = buildDocumentationModule(environment, moduleName, options, includes, { isSample(it) },
                packageDocumentationBuilder, null, logger)

        val timeAnalyse = System.currentTimeMillis() - startAnalyse
        logger.info("done in ${timeAnalyse / 1000} secs")

        val timeBuild = measureTimeMillis {
            logger.info("Generating pages... ")
            generator.buildAll(documentation)
        }
        logger.info("done in ${timeBuild / 1000} secs")

        Disposer.dispose(environment)
    }

    fun createAnalysisEnvironment(): AnalysisEnvironment {
        val environment = AnalysisEnvironment(DokkaMessageCollector(logger)) {
            addClasspath(PathUtil.getJdkClassesRoots())
            //   addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
            for (element in this@DokkaGenerator.classpath) {
                addClasspath(File(element))
            }

            addSources(this@DokkaGenerator.sources)
            addSources(this@DokkaGenerator.samples)
        }
        return environment
    }

    fun isSample(file: PsiFile): Boolean {
        val sourceFile = File(file.virtualFile!!.path)
        return samples.none { sample ->
            val canonicalSample = File(sample).canonicalPath
            val canonicalSource = sourceFile.canonicalPath
            canonicalSource.startsWith(canonicalSample)
        }
    }
}

fun buildDocumentationModule(environment: AnalysisEnvironment,
                             moduleName: String,
                             options: DocumentationOptions,
                             includes: List<String> = listOf(),
                             filesToDocumentFilter: (PsiFile) -> Boolean = { file -> true },
                             packageDocumentationBuilder: PackageDocumentationBuilder? = null,
                             javaDocumentationBuilder: JavaDocumentationBuilder? = null,
                             logger: DokkaLogger): DocumentationModule {
    val documentation = environment.withContext { coreEnvironment, resolutionFacade, session ->
        val fragmentFiles = coreEnvironment.getSourceFiles().filter(filesToDocumentFilter)
        val analyzer = resolutionFacade.getFrontendService(LazyTopDownAnalyzerForTopLevel::class.java)
        analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, fragmentFiles)

        val fragments = fragmentFiles.map { session.getPackageFragment(it.packageFqName) }.filterNotNull().distinct()

        val refGraph = NodeReferenceGraph()
        val linkResolver = DeclarationLinkResolver(resolutionFacade, refGraph, logger)
        val documentationBuilder = DocumentationBuilder(resolutionFacade, session, linkResolver, options, refGraph, logger)
        val packageDocs = PackageDocs(linkResolver, fragments.firstOrNull(), logger)
        for (include in includes) {
            packageDocs.parse(include)
        }
        val documentationModule = DocumentationModule(moduleName, packageDocs.moduleContent)

        with(documentationBuilder) {
            if (packageDocumentationBuilder != null) {
                documentationModule.appendFragments(fragments, packageDocs.packageContent, packageDocumentationBuilder)
            }
            else {
                documentationModule.appendFragments(fragments, packageDocs.packageContent)
            }
        }

        val javaFiles = coreEnvironment.getJavaSourceFiles().filter(filesToDocumentFilter)
        with(javaDocumentationBuilder ?: documentationBuilder) {
            javaFiles.map { appendFile(it, documentationModule, packageDocs.packageContent) }
        }

        refGraph.resolveReferences()

        documentationModule
    }

    return documentation
}


fun KotlinCoreEnvironment.getJavaSourceFiles(): List<PsiJavaFile> {
    val sourceRoots = configuration.get(CommonConfigurationKeys.CONTENT_ROOTS)
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
