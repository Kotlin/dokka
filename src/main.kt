package com.jetbrains.dokka

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
        addClasspath(PathUtil.getJdkClassesRoots())
        addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())

        addSources(sources)
    }

    println("Dokka is preparing sources and libraries...")
    println("Sources: ${environment.sources.join()}")
    println("Classpath: ${environment.classpath.joinToString()}")

    println()

    val results = environment.processFiles { context, file ->
        println("Processing: ${file.getName()}")
        println()
        context.analyseFile(file)
    }

    println()
    println("Results:")
    results.forEach {
        println(it)
    }

    Disposer.dispose(environment)
}


fun BindingContext.analyseFile(file: JetFile) {
    val packageFragment = getPackageFragment(file)
    if (packageFragment == null) {
        println("PackageFragment is null")
        return
    }

    println("Package: ${packageFragment}")
    for (descriptor in packageFragment.getMemberScope().getAllDescriptors()) {
        println("Member: ${descriptor}")
    }
}
