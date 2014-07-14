package org.jetbrains.dokka.tests

import org.jetbrains.jet.cli.common.messages.*
import com.intellij.openapi.util.*
import kotlin.test.fail
import org.jetbrains.dokka.*

public fun verifyModel(vararg files: String, verifier: (DocumentationModule) -> Unit) {
    val messageCollector = object : MessageCollector {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            when (severity) {
                CompilerMessageSeverity.WARNING,
                CompilerMessageSeverity.LOGGING,
                CompilerMessageSeverity.OUTPUT,
                CompilerMessageSeverity.INFO,
                CompilerMessageSeverity.ERROR -> {
                    println("$severity: $message at $location")
                }
                CompilerMessageSeverity.EXCEPTION -> {
                    fail("$severity: $message at $location")
                }
            }
        }
    }

    val environment = AnalysisEnvironment(messageCollector) {
        addSources(files.toList())
    }

    val documentation = environment.withContext<DocumentationModule> { environment, module, context ->
        val packageSet = environment.getSourceFiles().map { file ->
            context.getPackageFragment(file)!!.fqName
        }.toSet()

        context.createDocumentationModule(module, packageSet)
    }
    verifier(documentation)
    Disposer.dispose(environment)
}

