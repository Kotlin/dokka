package org.jetbrains.dokka.tests

import org.jetbrains.jet.cli.common.messages.*
import com.intellij.openapi.util.*
import kotlin.test.fail
import org.jetbrains.dokka.*
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

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

    val options = DocumentationOptions(includeNonPublic = true)

    val documentation = environment.withContext { environment, session ->
        val fragments = environment.getSourceFiles().map { session.getPackageFragment(it.getPackageFqName()) }.filterNotNull().distinct()
        val descriptors = hashMapOf<String, List<DeclarationDescriptor>>()
        for ((name, parts) in fragments.groupBy { it.fqName }) {
            descriptors.put(name.asString(), parts.flatMap { it.getMemberScope().getAllDescriptors() })
        }

        val documentationModule = DocumentationModule("test")
        val documentationBuilder = DocumentationBuilder(session, options)
        with(documentationBuilder) {
            documentationModule.appendFragments(fragments)
        }
        documentationBuilder.resolveReferences(documentationModule)
        documentationModule
    }
    verifier(documentation)
    Disposer.dispose(environment)
}

fun StringBuilder.appendChildren(node: ContentNode): StringBuilder {
    for (child in node.children) {
        val childText = child.toTestString()
        append(childText)
    }
    return this
}

fun StringBuilder.appendNode(node: ContentNode): StringBuilder {
    when (node) {
        is ContentText -> {
            append(node.text)
        }
        is ContentEmphasis -> append("*").appendChildren(node).append("*")
        else -> {
            appendChildren(node)
        }
    }
    return this
}

fun ContentNode.toTestString(): String {
    val node = this
    return StringBuilder {
        appendNode(node)
    }.toString()
}
