package org.jetbrains.dokka.tests

import org.jetbrains.jet.cli.common.messages.*
import com.intellij.openapi.util.*
import com.jetbrains.dokka.*
import kotlin.test.fail

public fun verifyModel(vararg files: String, verifier: (DocumentationModel) -> Unit) {
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

    val environment = AnalysesEnvironment(messageCollector) {
        addSources(files.toList())
    }

    val result = environment.processFiles { context, file ->
        context.createDocumentation(file)
    }.fold(DocumentationModel()) {(aggregate, item) -> aggregate.merge(item) }
    verifier(result)
    Disposer.dispose(environment)
}

