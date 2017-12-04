package org.jetbrains.dokka.tests

import com.google.inject.Guice
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.SourceLinkDefinition
import org.jetbrains.dokka.Utilities.DokkaAnalysisModule
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.junit.Assert
import org.junit.Assert.fail
import java.io.File

fun verifyModel(vararg roots: ContentRoot,
                withJdk: Boolean = false,
                withKotlinRuntime: Boolean = false,
                format: String = "html",
                includeNonPublic: Boolean = true,
                perPackageOptions: List<DokkaConfiguration.PackageOptions> = emptyList(),
                verifier: (DocumentationModule) -> Unit) {
    val documentation = DocumentationModule("test")

    val options = DocumentationOptions(
            "",
            format,
            includeNonPublic = includeNonPublic,
            skipEmptyPackages = false,
            includeRootPackage = true,
            sourceLinks = listOf(),
            perPackageOptions = perPackageOptions,
            generateIndexPages = false,
            noStdlibLink = true,
            cacheRoot = "default",
            languageVersion = null,
            apiVersion = null
    )

    appendDocumentation(documentation, *roots,
            withJdk = withJdk,
            withKotlinRuntime = withKotlinRuntime,
            options = options)
    documentation.prepareForGeneration(options)

    verifier(documentation)
}

fun appendDocumentation(documentation: DocumentationModule,
                        vararg roots: ContentRoot,
                        withJdk: Boolean = false,
                        withKotlinRuntime: Boolean = false,
                        options: DocumentationOptions,
                        defaultPlatforms: List<String> = emptyList()) {
    val messageCollector = object : MessageCollector {
        override fun clear() {

        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            when (severity) {
                CompilerMessageSeverity.STRONG_WARNING,
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
            val kotlinStrictfpRoot = PathManager.getResourceRoot(Strictfp::class.java, "/kotlin/jvm/Strictfp.class")
            addClasspath(File(kotlinStrictfpRoot))
        }
        addRoots(roots.toList())

        loadLanguageVersionSettings(options.languageVersion, options.apiVersion)
    }
    val defaultPlatformsProvider = object : DefaultPlatformsProvider {
        override fun getDefaultPlatforms(descriptor: DeclarationDescriptor) = defaultPlatforms
    }
    val injector = Guice.createInjector(
            DokkaAnalysisModule(environment, options, defaultPlatformsProvider, documentation.nodeRefGraph, DokkaConsoleLogger))
    buildDocumentationModule(injector, documentation)
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
                 format: String = "html",
                 includeNonPublic: Boolean = true,
                 outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyModel(
            *roots,
            withJdk = withJdk,
            withKotlinRuntime = withKotlinRuntime,
            format = format,
            includeNonPublic = includeNonPublic
    ) {
        verifyModelOutput(it, outputExtension, roots.first().path, outputGenerator)
    }
}

fun verifyModelOutput(it: DocumentationModule,
                      outputExtension: String,
                      sourcePath: String,
                      outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    val output = StringBuilder()
    outputGenerator(it, output)
    val ext = outputExtension.removePrefix(".")
    val expectedFile = File(sourcePath.replaceAfterLast(".", ext, sourcePath + "." + ext))
    assertEqualsIgnoringSeparators(expectedFile, output.toString())
}

fun verifyOutput(path: String,
                 outputExtension: String,
                 withJdk: Boolean = false,
                 withKotlinRuntime: Boolean = false,
                 format: String = "html",
                 includeNonPublic: Boolean = true,
                 outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyOutput(
            arrayOf(contentRootFromPath(path)),
            outputExtension,
            withJdk,
            withKotlinRuntime,
            format,
            includeNonPublic,
            outputGenerator
    )
}

fun verifyJavaOutput(path: String,
                     outputExtension: String,
                     withKotlinRuntime: Boolean = false,
                     outputGenerator: (DocumentationModule, StringBuilder) -> Unit) {
    verifyJavaModel(path, withKotlinRuntime) { model ->
        verifyModelOutput(model, outputExtension, path, outputGenerator)
    }
}

fun assertEqualsIgnoringSeparators(expectedFile: File, output: String) {
    if (!expectedFile.exists()) expectedFile.createNewFile()
    val expectedText = expectedFile.readText().replace("\r\n", "\n")
    val actualText = output.replace("\r\n", "\n")

    if(expectedText != actualText)
        throw FileComparisonFailure("", expectedText, actualText, expectedFile.canonicalPath)
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

val ContentRoot.path: String
    get() = when(this) {
        is KotlinSourceRoot -> path
        is JavaSourceRoot -> file.path
        else -> throw UnsupportedOperationException()
    }
