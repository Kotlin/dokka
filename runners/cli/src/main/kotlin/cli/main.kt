package org.jetbrains.dokka

import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.kotlin.cli.common.arguments.ValueDescription
import java.io.File

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

