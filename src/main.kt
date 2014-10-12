package org.jetbrains.dokka

import com.sampullara.cli.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.common.arguments.*
import org.jetbrains.jet.utils.PathUtil
import java.io.File
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

class DokkaArguments {
    Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    ValueDescription("<path>")
    public var src: String? = null

    Argument(value = "output", description = "Output directory path for .md files")
    ValueDescription("<path>")
    public var outputDir: String = "out/doc/"

    Argument(value = "module", description = "Name of the documentation module")
    ValueDescription("<name>")
    public var moduleName: String = ""

    Argument(value = "classpath", description = "Classpath for symbol resolution")
    ValueDescription("<path>")
    public var classpath: String = ""
}

public fun main(args: Array<String>) {

    val arguments = DokkaArguments()
    val sourceFiles = Args.parse(arguments, args)
    val sources: List<String> = sourceFiles ?: listOf()

    val environment = AnalysisEnvironment(MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR) {
        addClasspath(PathUtil.getJdkClassesRoots())
        for (element in arguments.classpath.split(File.pathSeparatorChar)) {
            addClasspath(File(element))
        }
        //   addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
        addSources(sources)
    }

    println("Module: ${arguments.moduleName}")
    println("Output: ${arguments.outputDir}")
    println("Sources: ${environment.sources.join()}")
    println("Classpath: ${environment.classpath.joinToString()}")

    println()

    println("Analysing sources and libraries... ")
    val startAnalyse = System.currentTimeMillis()

    val documentation = environment.withContext { environment, module, context ->
        val fragments = environment.getSourceFiles().map { context.getPackageFragment(it) }.filterNotNull().distinct()
        val documentationModule = DocumentationModule(arguments.moduleName)
        val options = DocumentationOptions()
        val documentationBuilder = DocumentationBuilder(context, options)

        with(documentationBuilder) {
            val descriptors = hashMapOf<String, List<DeclarationDescriptor>>()
            for ((name, parts) in fragments.groupBy { it.fqName }) {
                descriptors.put(name.asString(), parts.flatMap { it.getMemberScope().getAllDescriptors() })
            }
            for ((packageName, declarations) in descriptors) {
                println("  package $packageName: ${declarations.count()} nodes")
                val packageNode = DocumentationNode(packageName, Content.Empty, DocumentationNode.Kind.Package)
                packageNode.appendChildren(declarations, DocumentationReference.Kind.Member)
                documentationModule.append(packageNode, DocumentationReference.Kind.Member)
            }
        }

        documentationBuilder.resolveReferences(documentationModule)
        documentationModule
    }

    val timeAnalyse = System.currentTimeMillis() - startAnalyse
    println("done in ${timeAnalyse / 1000} secs")

    val startBuild = System.currentTimeMillis()
    val signatureGenerator = KotlinLanguageService()
    val locationService = FoldersLocationService(arguments.outputDir)
    val templateService = HtmlTemplateService.default("/dokka/styles/style.css")

    val formatter = HtmlFormatService(locationService, signatureGenerator, templateService)
    val generator = FileGenerator(signatureGenerator, locationService, formatter)
    print("Generating pages... ")
    generator.buildPage(documentation)
    generator.buildOutline(documentation)
    val timeBuild = System.currentTimeMillis() - startBuild
    println("done in ${timeBuild / 1000} secs")
    println()
    println("Done.")
    Disposer.dispose(environment)
}