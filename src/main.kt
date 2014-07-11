package org.jetbrains.dokka

import com.sampullara.cli.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.common.arguments.*
import org.jetbrains.jet.utils.PathUtil

class DokkaArguments {
    Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    ValueDescription("<path>")
    public var src: String? = null

    Argument(value = "output", description = "Output directory path for .md files")
    ValueDescription("<path>")
    public var outputDir: String? = null
}

public fun main(args: Array<String>) {

    val arguments = DokkaArguments()
    val sourceFiles = Args.parse(arguments, args)
    val sources: List<String> = sourceFiles ?: listOf()

    val environment = AnalysisEnvironment(MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR) {
        /*
                addClasspath(PathUtil.getJdkClassesRoots())
        */
        addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
        addSources(sources)
    }

    println("Dokka is preparing sources and libraries...")
    println("Sources: ${environment.sources.join()}")
    println("Classpath: ${environment.classpath.joinToString()}")

    println()

    val model = environment.processFiles { context, file ->
        println("Processing: ${file.getName()}")
        context.createDocumentationModel(file)
    }.fold(DocumentationModel()) {(aggregate, item) -> aggregate.merge(item) }

    ConsoleGenerator().generate(model)

    Disposer.dispose(environment)
}