package org.jetbrains.dokka

import com.sampullara.cli.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.utils.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments

public fun main(args: Array<String>) {

    val compilerArguments = K2JVMCompilerArguments()
    val sources: List<String> = Args.parse(compilerArguments, args) ?: listOf()

    val environment = AnalysisEnvironment(MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR) {
        /*
                addClasspath(PathUtil.getJdkClassesRoots())
                addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
        */
        addSources(sources)
    }

    println("Dokka is preparing sources and libraries...")
    println("Sources: ${environment.sources.join()}")
    println("Classpath: ${environment.classpath.joinToString()}")

    println()

    val model = environment.processFiles { context, file ->
        println("Processing: ${file.getName()}")
        context.createDocumentation(file)
    }.fold(DocumentationModel()) {(aggregate, item) -> aggregate.merge(item) }

    println(model)
    Disposer.dispose(environment)
}