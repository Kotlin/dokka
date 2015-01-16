package org.jetbrains.dokka.tests

import org.jetbrains.kotlin.cli.common.messages.*
import com.intellij.openapi.util.*
import kotlin.test.fail
import org.jetbrains.dokka.*
import java.io.File
import com.intellij.openapi.application.PathManager
import org.junit.Assert

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
        val stringRoot = PathManager.getResourceRoot(javaClass<String>(), "/java/lang/String.class")
        addClasspath(File(stringRoot))
        addSources(files.toList())
    }

    val options = DocumentationOptions(includeNonPublic = true, sourceLinks = listOf<SourceLinkDefinition>())

    val documentation = environment.withContext { environment, session ->
        val fragments = environment.getSourceFiles().map { session.getPackageFragment(it.getPackageFqName()) }.filterNotNull().distinct()

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

public fun verifyOutput(path: String, outputExtension: String, outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyModel(path) {
        val output = StringBuilder()
        outputGenerator(it, output)
        val expectedOutput = File(path.replace(".kt", outputExtension)).readText().replace("\r\n", "\n")
        Assert.assertEquals(expectedOutput, output.toString().replace("\r\n", "\n"))
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
        is ContentBlockCode -> {
            appendln("[code]")
            appendChildren(node)
            appendln()
            appendln("[/code]")
        }
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
    override fun location(node: DocumentationNode): Location {
        val (pageNode, anchor) = getParentPage(node)
        return Location(File("/tmp/out"), anchor)
    }
}
