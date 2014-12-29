package org.jetbrains.dokka.tests

import org.jetbrains.jet.cli.common.messages.*
import com.intellij.openapi.util.*
import kotlin.test.fail
import org.jetbrains.dokka.*
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import java.io.File
import kotlin.test.assertEquals

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

public fun verifyOutput(path: String, outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyModel(path) {
        val output = StringBuilder()
        outputGenerator(it, output)
        val trimmedOutput = output.toString().split('\n').map { it.trimTrailing() }.join("\n")
        val expectedOutput = File(path.replace(".kt", ".md")).readText()
        assertEquals(expectedOutput.trimTrailing(), trimmedOutput)
    }
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
        is ContentNodeLink -> {
            append("[")
            appendChildren(node)
            append(" -> ")
            append(node.node.toString())
            append("]")
        }
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

val tempLocation = Location(File("/tmp/out"))

object InMemoryLocationService: LocationService {
    override fun location(node: DocumentationNode) = tempLocation;
}
