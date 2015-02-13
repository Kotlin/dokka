package org.jetbrains.dokka

import com.sampullara.cli.*
import com.intellij.openapi.util.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFileManager

class DokkaArguments {
    Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    ValueDescription("<path>")
    public var src: String = ""

    Argument(value = "srcLink", description = "Mapping between a source directory and a Web site for browsing the code")
    ValueDescription("<path>=<url>[#lineSuffix]")
    public var srcLink: String = ""

    Argument(value = "include", description = "Markdown files to load (allows many paths separated by the system path separator)")
    ValueDescription("<path>")
    public var include: String = ""

    Argument(value = "samples", description = "Source root for samples")
    ValueDescription("<path>")
    public var samples: String = ""

    Argument(value = "output", description = "Output directory path")
    ValueDescription("<path>")
    public var outputDir: String = "out/doc/"

    Argument(value = "format", description = "Output format (text, html, markdown, jekyll, kotlin-website)")
    ValueDescription("<name>")
    public var outputFormat: String = "html"

    Argument(value = "module", description = "Name of the documentation module")
    ValueDescription("<name>")
    public var moduleName: String = ""

    Argument(value = "classpath", description = "Classpath for symbol resolution")
    ValueDescription("<path>")
    public var classpath: String = ""

}

class SourceLinkDefinition(val path: String, val url: String, val lineSuffix: String?)

private fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinition {
    val (path, urlAndLine) = srcLink.split('=')
    return SourceLinkDefinition(File(path).getAbsolutePath(),
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
            sourceLinks)

    generator.generate()
}

trait DokkaLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
}

object DokkaConsoleLogger: DokkaLogger {
    override fun info(message: String) = println(message)
    override fun warn(message: String) = println("WARN: $message")
    override fun error(message: String) = println("ERROR: $message")
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
                     val sourceLinks: List<SourceLinkDefinition>) {
    fun generate() {
        val environment = createAnalysisEnvironment()

        logger.info("Module: ${moduleName}")
        logger.info("Output: ${outputDir}")
        logger.info("Sources: ${environment.sources.join()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val options = DocumentationOptions(false, sourceLinks)
        val documentation = buildDocumentationModule(environment, moduleName, options, includes, { isSample(it) }, logger)

        val timeAnalyse = System.currentTimeMillis() - startAnalyse
        logger.info("done in ${timeAnalyse / 1000} secs")

        val startBuild = System.currentTimeMillis()
        val signatureGenerator = KotlinLanguageService()
        val locationService = FoldersLocationService(outputDir)
        val templateService = HtmlTemplateService.default("/dokka/styles/style.css")

        val (formatter, outlineFormatter) = when (outputFormat) {
            "html" -> {
                val htmlFormatService = HtmlFormatService(locationService, signatureGenerator, templateService)
                htmlFormatService to htmlFormatService
            }
            "markdown" -> MarkdownFormatService(locationService, signatureGenerator) to null
            "jekyll" -> JekyllFormatService(locationService, signatureGenerator) to null
            "kotlin-website" -> KotlinWebsiteFormatService(locationService, signatureGenerator) to
                    YamlOutlineService(locationService, signatureGenerator)
            else -> {
                logger.error("Unrecognized output format ${outputFormat}")
                null to null
            }
        }
        if (formatter == null) return

        val generator = FileGenerator(signatureGenerator, locationService, formatter, outlineFormatter)
        logger.info("Generating pages... ")
        generator.buildPage(documentation)
        generator.buildOutline(documentation)
        val timeBuild = System.currentTimeMillis() - startBuild
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
        val sourceFile = File(file.getVirtualFile()!!.getPath())
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
                             logger: DokkaLogger): DocumentationModule {
    val documentation = environment.withContext { environment, session ->
        val fragmentFiles = environment.getSourceFiles().filter(filesToDocumentFilter)
        val fragments = fragmentFiles.map { session.getPackageFragment(it.getPackageFqName()) }.filterNotNull().distinct()

        val moduleContent = Content()
        for (include in includes) {
            val file = File(include)
            if (file.exists()) {
                val text = file.readText()
                val tree = parseMarkdown(text)
                val content = buildContent(tree)
                moduleContent.children.addAll(content.children)
            } else {
                logger.warn("Include file $file was not found.")
            }
        }
        val documentationModule = DocumentationModule(moduleName, moduleContent)

        val documentationBuilder = DocumentationBuilder(session, options, logger)
        with(documentationBuilder) {
            documentationModule.appendFragments(fragments)
            documentationBuilder.resolveReferences(documentationModule)
        }

        val javaFiles = environment.getJavaSourceFiles().filter(filesToDocumentFilter)
        val javaDocumentationBuilder = JavaDocumentationBuilder()
        javaFiles.map { javaDocumentationBuilder.appendFile(it, documentationModule) }

        documentationModule
    }

    return documentation
}


fun JetCoreEnvironment.getJavaSourceFiles(): List<PsiJavaFile> {
    val sourceRoots = getConfiguration().getList(CommonConfigurationKeys.SOURCE_ROOTS_KEY).map { File(it) }
    val result = arrayListOf<PsiJavaFile>()
    val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")
    sourceRoots.forEach { sourceRoot ->
        sourceRoot.getAbsoluteFile().recurse {
            val vFile = localFileSystem.findFileByPath(it.path)
            if (vFile != null) {
                val psiFile = PsiManager.getInstance(getProject()).findFile(vFile)
                if (psiFile is PsiJavaFile) {
                    result.add(psiFile)
                }
            }
        }
    }
    return result
}
