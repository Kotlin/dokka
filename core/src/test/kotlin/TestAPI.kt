package org.jetbrains.dokka.tests

import com.google.inject.Guice
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.DokkaModule
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.junit.Assert
import org.junit.Assert.fail
import java.io.File

fun verifyModel(vararg roots: ContentRoot,
                withJdk: Boolean = false,
                withKotlinRuntime: Boolean = false,
                format: String = "html",
                includeNonPublic: Boolean = true,
                verifier: (DocumentationModule) -> Unit) {
    val messageCollector = object : MessageCollector {
        override fun clear() {

        }

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

        override fun hasErrors() = false
    }

    val environment = AnalysisEnvironment(messageCollector)
    environment.apply {
        if (withJdk || withKotlinRuntime) {
            val stringRoot = PathManager.getResourceRoot(String::class.java, "/java/lang/String.class")
            addClasspath(File(stringRoot))
        }
        if (withKotlinRuntime) {
            val kotlinPairRoot = PathManager.getResourceRoot(Pair::class.java, "/kotlin/Pair.class")
            addClasspath(File(kotlinPairRoot))

            val kotlinStrictfpRoot = PathManager.getResourceRoot(Strictfp::class.java, "/kotlin/jvm/Strictfp.class")
            addClasspath(File(kotlinStrictfpRoot))
        }
        addRoots(roots.toList())
    }
    val options = DocumentationOptions("", format,
            includeNonPublic = includeNonPublic,
            skipEmptyPackages = false,
            sourceLinks = listOf<SourceLinkDefinition>(),
            generateIndexPages = false)
    val injector = Guice.createInjector(DokkaModule(environment, options, DokkaConsoleLogger))
    val documentation = buildDocumentationModule(injector, "test")
    verifier(documentation)
    Disposer.dispose(environment)
}

fun verifyModel(source: String,
                withJdk: Boolean = false,
                withKotlinRuntime: Boolean = false,
                format: String = "html",
                includeNonPublic: Boolean = true,
                verifier: (DocumentationModule) -> Unit) {
    if (!File(source).exists()) {
        throw IllegalArgumentException("Can't find test data file $source")
    }
    verifyModel(contentRootFromPath(source),
            withJdk = withJdk,
            withKotlinRuntime = withKotlinRuntime,
            format = format,
            includeNonPublic = includeNonPublic,
            verifier = verifier)
}

fun verifyPackageMember(source: String,
                        withJdk: Boolean = false,
                        withKotlinRuntime: Boolean = false,
                        verifier: (DocumentationNode) -> Unit) {
    verifyModel(source, withJdk = withJdk, withKotlinRuntime = withKotlinRuntime) { model ->
        val pkg = model.members.single()
        verifier(pkg.members.single())
    }
}

fun verifyJavaModel(source: String,
                    withKotlinRuntime: Boolean = false,
                    verifier: (DocumentationModule) -> Unit) {
    val tempDir = FileUtil.createTempDirectory("dokka", "")
    try {
        val sourceFile = File(source)
        FileUtil.copy(sourceFile, File(tempDir, sourceFile.name))
        verifyModel(JavaSourceRoot(tempDir, null), withJdk = true, withKotlinRuntime = withKotlinRuntime, verifier = verifier)
    }
    finally {
        FileUtil.delete(tempDir)
    }
}

fun verifyJavaPackageMember(source: String,
                            withKotlinRuntime: Boolean = false,
                            verifier: (DocumentationNode) -> Unit) {
    verifyJavaModel(source, withKotlinRuntime) { model ->
        val pkg = model.members.single()
        verifier(pkg.members.single())
    }
}

fun verifyOutput(roots: Array<ContentRoot>,
                 outputExtension: String,
                 withJdk: Boolean = false,
                 withKotlinRuntime: Boolean = false,
                 outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyModel(*roots, withJdk = withJdk, withKotlinRuntime = withKotlinRuntime) {
        verifyModelOutput(it, outputExtension, outputGenerator, roots.first().path)
    }
}

private fun verifyModelOutput(it: DocumentationModule,
                              outputExtension: String,
                              outputGenerator: (DocumentationModule, StringBuilder) -> Unit,
                              sourcePath: String) {
    val output = StringBuilder()
    outputGenerator(it, output)
    val ext = outputExtension.removePrefix(".")
    val path = sourcePath
    val expectedOutput = File(path.replaceAfterLast(".", ext, path + "." + ext)).readText()
    assertEqualsIgnoringSeparators(expectedOutput, output.toString())
}

fun verifyOutput(path: String,
                 outputExtension: String,
                 withJdk: Boolean = false,
                 withKotlinRuntime: Boolean = false,
                 outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyOutput(arrayOf(contentRootFromPath(path)), outputExtension, withJdk, withKotlinRuntime, outputGenerator)
}

fun verifyJavaOutput(path: String,
                     outputExtension: String,
                     withKotlinRuntime: Boolean = false,
                     outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyJavaModel(path, withKotlinRuntime) { model ->
        verifyModelOutput(model, outputExtension, outputGenerator, path)
    }
}

fun assertEqualsIgnoringSeparators(expectedOutput: String, output: String) {
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
            if (node.language.isNotBlank())
                appendln("[code lang=${node.language}]")
            else
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
        is ContentEmpty -> { /* nothing */ }
        else -> throw IllegalStateException("Don't know how to format node $node")
    }
    return this
}

fun ContentNode.toTestString(): String {
    val node = this
    return StringBuilder().apply {
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

    override val root: Location
        get() = InMemoryLocation("")
}

val tempLocation = InMemoryLocation("")

val ContentRoot.path: String
    get() = when(this) {
        is KotlinSourceRoot -> path
        is JavaSourceRoot -> file.path
        else -> throw UnsupportedOperationException()
    }
