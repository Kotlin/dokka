package org.jetbrains.dokka

import com.google.inject.Guice
import com.google.inject.Injector
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.dokka.Utilities.DokkaModule
import org.jetbrains.kotlin.cli.common.arguments.ValueDescription
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.system.measureTimeMillis

class DokkaArguments {
    @set:Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    @ValueDescription("<path>")
    var src: String = ""

    @set:Argument(value = "srcLink", description = "Mapping between a source directory and a Web site for browsing the code")
    @ValueDescription("<path>=<url>[#lineSuffix]")
    var srcLink: String = ""

    @set:Argument(value = "include", description = "Markdown files to load (allows many paths separated by the system path separator)")
    @ValueDescription("<path>")
    var include: String = ""

    @set:Argument(value = "samples", description = "Source root for samples")
    @ValueDescription("<path>")
    var samples: String = ""

    @set:Argument(value = "output", description = "Output directory path")
    @ValueDescription("<path>")
    var outputDir: String = "out/doc/"

    @set:Argument(value = "format", description = "Output format (text, html, markdown, jekyll, kotlin-website)")
    @ValueDescription("<name>")
    var outputFormat: String = "html"

    @set:Argument(value = "module", description = "Name of the documentation module")
    @ValueDescription("<name>")
    var moduleName: String = ""

    @set:Argument(value = "classpath", description = "Classpath for symbol resolution")
    @ValueDescription("<path>")
    var classpath: String = ""

    @set:Argument(value = "nodeprecated", description = "Exclude deprecated members from documentation")
    var nodeprecated: Boolean = false

    @set:Argument(value = "jdkVersion", description = "Version of JDK to use for linking to JDK JavaDoc")
    var jdkVersion: Int = 6
}

private fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinition {
    val (path, urlAndLine) = srcLink.split('=')
    return SourceLinkDefinition(File(path).absolutePath,
            urlAndLine.substringBefore("#"),
            urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#" + it })
}

fun main(args: Array<String>) {
    val arguments = DokkaArguments()
    val freeArgs: List<String> = Args.parse(arguments, args, false) ?: listOf()
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

    val documentationOptions = DocumentationOptions(
            arguments.outputDir.let { if (it.endsWith('/')) it else it + '/' },
            arguments.outputFormat,
            skipDeprecated = arguments.nodeprecated,
            sourceLinks = sourceLinks
    )

    val generator = DokkaGenerator(
            DokkaConsoleLogger,
            classPath,
            sources,
            samples,
            includes,
            arguments.moduleName,
            documentationOptions)

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
            println("generation completed with $warningCount warnings")
        } else {
            println("generation completed successfully")
        }
    }
}

class DokkaMessageCollector(val logger: DokkaLogger): MessageCollector {
    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

class DokkaGenerator(val logger: DokkaLogger,
                     val classpath: List<String>,
                     val sources: List<String>,
                     val samples: List<String>,
                     val includes: List<String>,
                     val moduleName: String,
                     val options: DocumentationOptions) {
    fun generate() {
        val environment = createAnalysisEnvironment()

        logger.info("Module: $moduleName")
        logger.info("Output: ${File(options.outputDir)}")
        logger.info("Sources: ${environment.sources.joinToString()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val injector = Guice.createInjector(DokkaModule(environment, options, logger))

        val documentation = buildDocumentationModule(injector, moduleName, { isSample(it) }, includes)

        val timeAnalyse = System.currentTimeMillis() - startAnalyse
        logger.info("done in ${timeAnalyse / 1000} secs")

        val timeBuild = measureTimeMillis {
            logger.info("Generating pages... ")
            injector.getInstance(Generator::class.java).buildAll(documentation)
        }
        logger.info("done in ${timeBuild / 1000} secs")

        Disposer.dispose(environment)
    }

    fun createAnalysisEnvironment(): AnalysisEnvironment {
        val environment = AnalysisEnvironment(DokkaMessageCollector(logger))

        environment.apply {
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

fun buildDocumentationModule(injector: Injector,
                             moduleName: String,
                             filesToDocumentFilter: (PsiFile) -> Boolean = { file -> true },
                             includes: List<String> = listOf()): DocumentationModule {

    val coreEnvironment = injector.getInstance(KotlinCoreEnvironment::class.java)
    val fragmentFiles = coreEnvironment.getSourceFiles().filter(filesToDocumentFilter)

    val resolutionFacade = injector.getInstance(DokkaResolutionFacade::class.java)
    val analyzer = resolutionFacade.getFrontendService(LazyTopDownAnalyzerForTopLevel::class.java)
    analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, fragmentFiles)

    val fragments = fragmentFiles
            .map { resolutionFacade.resolveSession.getPackageFragment(it.packageFqName) }
            .filterNotNull()
            .distinct()

    val packageDocs = injector.getInstance(PackageDocs::class.java)
    for (include in includes) {
        packageDocs.parse(include, fragments.firstOrNull())
    }
    val documentationModule = DocumentationModule(moduleName, packageDocs.moduleContent)

    with(injector.getInstance(DocumentationBuilder::class.java)) {
        documentationModule.appendFragments(fragments, packageDocs.packageContent,
                injector.getInstance(PackageDocumentationBuilder::class.java))
    }

    val javaFiles = coreEnvironment.getJavaSourceFiles().filter(filesToDocumentFilter)
    with(injector.getInstance(JavaDocumentationBuilder::class.java)) {
        javaFiles.map { appendFile(it, documentationModule, packageDocs.packageContent) }
    }

    injector.getInstance(NodeReferenceGraph::class.java).resolveReferences()

    return documentationModule
}


fun KotlinCoreEnvironment.getJavaSourceFiles(): List<PsiJavaFile> {
    val sourceRoots = configuration.get(JVMConfigurationKeys.CONTENT_ROOTS)
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
