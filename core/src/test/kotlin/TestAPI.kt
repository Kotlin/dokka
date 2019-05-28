package org.jetbrains.dokka.tests

import com.google.inject.Guice
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.DokkaAnalysisModule
import org.jetbrains.dokka.Utilities.DokkaRunModule
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Assert.fail
import java.io.File

data class ModelConfig(
    val roots: Array<ContentRoot> = arrayOf(),
    val withJdk: Boolean = false,
    val withKotlinRuntime: Boolean = false,
    val format: String = "html",
    val includeNonPublic: Boolean = true,
    val perPackageOptions: List<DokkaConfiguration.PackageOptions> = emptyList(),
    val analysisPlatform: Platform = Platform.DEFAULT,
    val defaultPlatforms: List<String> = emptyList(),
    val noStdlibLink: Boolean = true,
    val collectInheritedExtensionsFromLibraries: Boolean = false,
    val sourceLinks: List<DokkaConfiguration.SourceLinkDefinition> = emptyList()
)

fun verifyModel(
    modelConfig: ModelConfig,
    verifier: (DocumentationModule) -> Unit
) {
    val documentation = DocumentationModule("test")

    val passConfiguration = PassConfigurationImpl(
        includeNonPublic = modelConfig.includeNonPublic,
        skipEmptyPackages = false,
        includeRootPackage = true,
        sourceLinks = modelConfig.sourceLinks,
        perPackageOptions = modelConfig.perPackageOptions,
        noStdlibLink = modelConfig.noStdlibLink,
        noJdkLink = false,
        languageVersion = null,
        apiVersion = null,
        collectInheritedExtensionsFromLibraries = modelConfig.collectInheritedExtensionsFromLibraries
    )
    val configuration = DokkaConfigurationImpl(
        outputDir = "",
        format = modelConfig.format,
        generateIndexPages = false,
        cacheRoot = "default",
        passesConfigurations = listOf(passConfiguration)
    )

    appendDocumentation(documentation, configuration, passConfiguration, modelConfig)
    documentation.prepareForGeneration(configuration)

    verifier(documentation)
}

fun appendDocumentation(
    documentation: DocumentationModule,
    dokkaConfiguration: DokkaConfiguration,
    passConfiguration: DokkaConfiguration.PassConfiguration,
    modelConfig: ModelConfig
) {
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

    val environment = AnalysisEnvironment(messageCollector, modelConfig.analysisPlatform)
    environment.apply {
        if (modelConfig.withJdk || modelConfig.withKotlinRuntime) {
            addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
        }
        if (modelConfig.withKotlinRuntime) {
            if (analysisPlatform == Platform.jvm) {
                val kotlinStrictfpRoot = PathManager.getResourceRoot(Strictfp::class.java, "/kotlin/jvm/Strictfp.class")
                addClasspath(File(kotlinStrictfpRoot))
            }
            if (analysisPlatform == Platform.js) {
                val kotlinStdlibJsRoot = PathManager.getResourceRoot(Any::class.java, "/kotlin/jquery")
                addClasspath(File(kotlinStdlibJsRoot))
            }

            if (analysisPlatform == Platform.common) {
                // TODO: Feels hacky
                val kotlinStdlibCommonRoot = ClassLoader.getSystemResource("kotlin/UInt.kotlin_metadata")
                addClasspath(File(kotlinStdlibCommonRoot.file.replace("file:", "").replaceAfter(".jar", "")))
            }
        }
        addRoots(modelConfig.roots.toList())

        loadLanguageVersionSettings(passConfiguration.languageVersion, passConfiguration.apiVersion)
    }
    val defaultPlatformsProvider = object : DefaultPlatformsProvider {
        override fun getDefaultPlatforms(descriptor: DeclarationDescriptor) = modelConfig.defaultPlatforms
    }

    val globalInjector = Guice.createInjector(
        DokkaRunModule(dokkaConfiguration)
    )

    val injector = globalInjector.createChildInjector(
        DokkaAnalysisModule(
            environment,
            dokkaConfiguration,
            defaultPlatformsProvider,
            documentation.nodeRefGraph,
            passConfiguration,
            DokkaConsoleLogger
        )
    )

    buildDocumentationModule(injector, documentation)
    Disposer.dispose(environment)
}

fun checkSourceExistsAndVerifyModel(
    source: String,
    modelConfig: ModelConfig = ModelConfig(),
    verifier: (DocumentationModule) -> Unit
) {
    require(File(source).exists()) {
        "Cannot find test data file $source"
    }
    verifyModel(
        ModelConfig(
            roots = arrayOf(contentRootFromPath(source)),
            withJdk = modelConfig.withJdk,
            withKotlinRuntime = modelConfig.withKotlinRuntime,
            format = modelConfig.format,
            includeNonPublic = modelConfig.includeNonPublic,
            sourceLinks = modelConfig.sourceLinks,
            analysisPlatform = modelConfig.analysisPlatform
        ),

        verifier = verifier
    )
}

fun verifyPackageMember(
    source: String,
    modelConfig: ModelConfig = ModelConfig(),
    verifier: (DocumentationNode) -> Unit
) {
    checkSourceExistsAndVerifyModel(
        source,
        modelConfig = ModelConfig(
            withJdk = modelConfig.withJdk,
            withKotlinRuntime = modelConfig.withKotlinRuntime,
            analysisPlatform = modelConfig.analysisPlatform
        )
    ) { model ->
        val pkg = model.members.single()
        verifier(pkg.members.single())
    }
}

fun verifyJavaModel(
    source: String,
    modelConfig: ModelConfig = ModelConfig(),
    verifier: (DocumentationModule) -> Unit
) {
    val tempDir = FileUtil.createTempDirectory("dokka", "")
    try {
        val sourceFile = File(source)
        FileUtil.copy(sourceFile, File(tempDir, sourceFile.name))
        verifyModel(
            ModelConfig(
                roots = arrayOf(JavaSourceRoot(tempDir, null)),
                withJdk = true,
                withKotlinRuntime = modelConfig.withKotlinRuntime,
                analysisPlatform = modelConfig.analysisPlatform
            ),
            verifier = verifier
        )
    } finally {
        FileUtil.delete(tempDir)
    }
}

fun verifyJavaPackageMember(
    source: String,
    modelConfig: ModelConfig = ModelConfig(),
    verifier: (DocumentationNode) -> Unit
) {
    verifyJavaModel(source, modelConfig) { model ->
        val pkg = model.members.single()
        verifier(pkg.members.single())
    }
}

fun verifyOutput(
    modelConfig: ModelConfig = ModelConfig(),
    outputExtension: String,
    outputGenerator: (DocumentationModule, StringBuilder) -> Unit
) {
    verifyModel(modelConfig) {
        verifyModelOutput(it, outputExtension, modelConfig.roots.first().path, outputGenerator)
    }
}

fun verifyOutput(
    path: String,
    outputExtension: String,
    modelConfig: ModelConfig = ModelConfig(),
    outputGenerator: (DocumentationModule, StringBuilder) -> Unit
) {
    verifyOutput(
        ModelConfig(
            roots = arrayOf(contentRootFromPath(path)) + modelConfig.roots,
            withJdk = modelConfig.withJdk,
            withKotlinRuntime = modelConfig.withKotlinRuntime,
            format = modelConfig.format,
            includeNonPublic = modelConfig.includeNonPublic,
            analysisPlatform = modelConfig.analysisPlatform,
            noStdlibLink = modelConfig.noStdlibLink,
            collectInheritedExtensionsFromLibraries = modelConfig.collectInheritedExtensionsFromLibraries
        ),
        outputExtension,
        outputGenerator
    )
}

fun verifyModelOutput(
    it: DocumentationModule,
    outputExtension: String,
    sourcePath: String,
    outputGenerator: (DocumentationModule, StringBuilder) -> Unit
) {
    val output = StringBuilder()
    outputGenerator(it, output)
    val ext = outputExtension.removePrefix(".")
    val expectedFile = File(sourcePath.replaceAfterLast(".", ext, sourcePath + "." + ext))
    assertEqualsIgnoringSeparators(expectedFile, output.toString())
}

fun verifyJavaOutput(
    path: String,
    outputExtension: String,
    modelConfig: ModelConfig = ModelConfig(),
    outputGenerator: (DocumentationModule, StringBuilder) -> Unit
) {
    verifyJavaModel(path, modelConfig) { model ->
        verifyModelOutput(model, outputExtension, path, outputGenerator)
    }
}

fun assertEqualsIgnoringSeparators(expectedFile: File, output: String) {
    if (!expectedFile.exists()) expectedFile.createNewFile()
    val expectedText = expectedFile.readText().replace("\r\n", "\n")
    val actualText = output.replace("\r\n", "\n")

    if (expectedText != actualText)
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
        is NodeRenderContent -> {
            append("render(")
            append(node.node)
            append(",")
            append(node.mode)
            append(")")
        }
        is ContentSymbol -> {
            append(node.text)
        }
        is ContentEmpty -> { /* nothing */
        }
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
    get() = when (this) {
        is KotlinSourceRoot -> path
        is JavaSourceRoot -> file.path
        else -> throw UnsupportedOperationException()
    }
