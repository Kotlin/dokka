package org.jetbrains.dokka.tests

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Disposer
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.junit.Assert
import java.io.File
import kotlin.test.fail

public fun verifyModel(vararg roots: ContentRoot, verifier: (DocumentationModule) -> Unit) {
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
        val stringRoot = PathManager.getResourceRoot(String::class.java, "/java/lang/String.class")
        addClasspath(File(stringRoot))
        val kotlinPairRoot = PathManager.getResourceRoot(Pair::class.java, "/kotlin/Pair.class")
        addClasspath(File(kotlinPairRoot))
        addRoots(roots.toList())
    }
    val options = DocumentationOptions(includeNonPublic = true, skipEmptyPackages = false, sourceLinks = listOf<SourceLinkDefinition>())
    val documentation = buildDocumentationModule(environment, "test", options, logger = DokkaConsoleLogger)
    verifier(documentation)
    Disposer.dispose(environment)
}

public fun verifyModel(source: String, verifier: (DocumentationModule) -> Unit) {
    verifyModel(contentRootFromPath(source), verifier = verifier)
}

public fun verifyPackageMember(kotlinSource: String, verifier: (DocumentationNode) -> Unit) {
    verifyModel(kotlinSource) { model ->
        val pkg = model.members.single()
        verifier(pkg.members.single())
    }
}

public fun verifyOutput(roots: Array<ContentRoot>, outputExtension: String, outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyModel(*roots) {
        val output = StringBuilder()
        outputGenerator(it, output)
        val ext = outputExtension.removePrefix(".")
        val path = roots.first().path
        val expectedOutput = File(path.replaceAfterLast(".", ext, path + "." + ext)).readText()
        assertEqualsIgnoringSeparators(expectedOutput, output.toString())
    }
}

public fun verifyOutput(path: String, outputExtension: String, outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyOutput(arrayOf(contentRootFromPath(path)), outputExtension, outputGenerator)
}

public fun assertEqualsIgnoringSeparators(expectedOutput: String, output: String) {
    Assert.assertEquals(expectedOutput.replace("\r\n", "\n"), output.replace("\r\n", "\n"))
}

fun StringBuilder.appendChildren(node: ContentBlock): StringBuilder {
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
        is ContentBlock -> {
            appendChildren(node)
        }
        else -> throw IllegalStateException("Don't know how to format node $node")
    }
    return this
}

fun ContentNode.toTestString(): String {
    val node = this
    return StringBuilder {
        appendNode(node)
    }.toString()
}

class InMemoryLocation(override val path: String): Location {
    override fun relativePathTo(other: Location, anchor: String?): String =
            if (anchor != null) other.path + "#" + anchor else other.path
}

object InMemoryLocationService: LocationService {
    override fun location(qualifiedName: List<String>, hasMembers: Boolean) =
            InMemoryLocation(relativePathToNode(qualifiedName, hasMembers))
}

val tempLocation = InMemoryLocation("")

val ContentRoot.path: String
    get() = when(this) {
        is KotlinSourceRoot -> path
        is JavaSourceRoot -> file.path
        else -> throw UnsupportedOperationException()
    }
