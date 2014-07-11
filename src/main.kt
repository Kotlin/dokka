package com.jetbrains.dokka

import org.jetbrains.jet.cli.common.arguments.*
import com.sampullara.cli.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.utils.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile

public fun main(args: Array<String>) {
    val dokka = DokkaContext(MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR)

    dokka.addClasspath(getClasspath(PathUtil.getKotlinPathsForCompiler()))
    val arguments = K2JVMCompilerArguments()
    val sources: List<String> = Args.parse(arguments, args) ?: listOf()
    dokka.addSources(sources)

    println("Dokka is preparing sources and libraries...")
    println("Sources: ${dokka.sources.join()}")
    println("Classpath: ${dokka.classpath.joinToString()}")

    println()

    dokka.analyzeFiles { context, file ->
        println("Processing: ${file.getName()}")
        println()
        analyseFile(context, file)
    }

    Disposer.dispose(dokka)
}

fun analyseFile(context: BindingContext, file: JetFile) {
    val packageFragment = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)
    if (packageFragment == null) {
        println("PackageFragment is null")
        return
    }

    println("Package: ${packageFragment}")
    for (descriptor in packageFragment.getMemberScope().getAllDescriptors()) {
        println("Member: ${descriptor}")
    }
}
