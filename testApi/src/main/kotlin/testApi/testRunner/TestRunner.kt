package org.jetbrains.dokka.testApi.testRunner

import com.intellij.openapi.application.PathManager
import org.jetbrains.dokka.*
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

// TODO: take dokka configuration from file
abstract class AbstractCoreTest {
    protected val logger = DokkaConsoleLogger

    protected fun getTestDataDir(name: String) =
        File("src/test/resources/$name").takeIf { it.exists() }?.toPath()
            ?: throw InvalidPathException(name, "Cannot be found")

    protected fun testFromData(
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        pluginOverrides: List<DokkaPlugin> = emptyList(),
        block: TestBuilder.() -> Unit
    ) {
        val testMethods = TestBuilder().apply(block).build()
        val tempDir = getTempDir(cleanupOutput)
        if (!cleanupOutput)
            logger.info("Output generated under: ${tempDir.root.absolutePath}")
        val newConfiguration =
            configuration.copy(
                outputDir = tempDir.root.toPath().toAbsolutePath().toString()
            )
        DokkaTestGenerator(
            newConfiguration,
            logger,
            testMethods,
            pluginOverrides
        ).generate()
    }

    protected fun testInline(
        query: String,
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        pluginOverrides: List<DokkaPlugin> = emptyList(),
        block: TestBuilder.() -> Unit
    ) {
        val testMethods = TestBuilder().apply(block).build()
        val testDirPath = getTempDir(cleanupOutput).root.toPath()
        val fileMap = query//.replace("""\n\s*\n?""".toRegex(), "\n")
            .replace("""\|/[^\w]""".toRegex()) { it.value.replace("|/", "| /") }.toFileMap()
        fileMap.materializeFiles(testDirPath.toAbsolutePath())
        if (!cleanupOutput)
            logger.info("Output generated under: ${testDirPath.toAbsolutePath()}")
        val newConfiguration =
            configuration.copy(
                outputDir = testDirPath.toAbsolutePath().toString(),
                passesConfigurations = configuration.passesConfigurations.map {
                    it.copy(sourceRoots = it.sourceRoots.map { it.copy(path = "${testDirPath.toAbsolutePath()}/${it.path}") })
                }
            )
        DokkaTestGenerator(
            newConfiguration,
            logger,
            testMethods,
            pluginOverrides
        ).generate()
    }

    private fun String.toFileMap(): Map<String, String> = this.trimMargin().removePrefix("|")
        .replace("\r\n", "\n")
        .split("\n/")
        .map { fileString ->
            fileString.split("\n", limit = 2)
                .let {
                    it.first().trim().removePrefix("/") to it.last().trim()
                }
        }.toMap()

    private fun Map<String, String>.materializeFiles(
        root: Path = Paths.get("."),
        charset: Charset = Charset.forName("utf-8")
    ) = this.map { (path, content) ->
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.write(file, content.toByteArray(charset))
    }

    private fun getTempDir(cleanupOutput: Boolean) = if (cleanupOutput) {
        TemporaryFolder().apply { create() }
    } else {
        object : TemporaryFolder() {
            override fun after() {}
        }.apply { create() }
    }

    protected class TestBuilder {
        var analysisSetupStage: (Map<SourceSetData, EnvironmentAndFacade>) -> Unit = {}
        var pluginsSetupStage: (DokkaContext) -> Unit = {}
        var documentablesCreationStage: (List<DModule>) -> Unit = {}
        var documentablesFirstTransformationStep: (List<DModule>) -> Unit = {}
        var documentablesMergingStage: (DModule) -> Unit = {}
        var documentablesTransformationStage: (DModule) -> Unit = {}
        var pagesGenerationStage: (ModulePageNode) -> Unit = {}
        var pagesTransformationStage: (RootPageNode) -> Unit = {}
        var renderingStage: (RootPageNode, DokkaContext) -> Unit = { a, b -> }

        fun build() = TestMethods(
            analysisSetupStage,
            pluginsSetupStage,
            documentablesCreationStage,
            documentablesFirstTransformationStep,
            documentablesMergingStage,
            documentablesTransformationStage,
            pagesGenerationStage,
            pagesTransformationStage,
            renderingStage
        )
    }

    protected fun dokkaConfiguration(block: DokkaConfigurationBuilder.() -> Unit): DokkaConfigurationImpl =
        DokkaConfigurationBuilder().apply(block).build()

    @DslMarker
    protected annotation class DokkaConfigurationDsl

    @DokkaConfigurationDsl
    protected class DokkaConfigurationBuilder {
        var outputDir: String = "out"
        var format: String = "html"
        var generateIndexPages: Boolean = true
        var cacheRoot: String? = null
        var pluginsClasspath: List<File> = emptyList()
        var pluginsConfigurations: Map<String, String> = emptyMap()
        private val passesConfigurations = mutableListOf<PassConfigurationImpl>()
        fun build() = DokkaConfigurationImpl(
            outputDir = outputDir,
            format = format,
            generateIndexPages = generateIndexPages,
            cacheRoot = cacheRoot,
            impliedPlatforms = emptyList(),
            passesConfigurations = passesConfigurations,
            pluginsClasspath = pluginsClasspath,
            pluginsConfiguration = pluginsConfigurations,
            modules = emptyList()
        )

        fun passes(block: Passes.() -> Unit) {
            passesConfigurations.addAll(Passes().apply(block))
        }
    }

    @DokkaConfigurationDsl
    protected class Passes : ArrayList<PassConfigurationImpl>() {
        fun pass(block: DokkaPassConfigurationBuilder.() -> Unit) =
            add(DokkaPassConfigurationBuilder().apply(block).build())
    }

    @DokkaConfigurationDsl
    protected class DokkaPassConfigurationBuilder(
        var moduleName: String = "root",
        var sourceSetName: String = "main",
        var classpath: List<String> = emptyList(),
        var sourceRoots: List<String> = emptyList(),
        var dependentSourceRoots: List<String> = emptyList(),
        var dependentSourceSets: List<String> = emptyList(),
        var samples: List<String> = emptyList(),
        var includes: List<String> = emptyList(),
        var includeNonPublic: Boolean = true,
        var includeRootPackage: Boolean = true,
        var reportUndocumented: Boolean = false,
        var skipEmptyPackages: Boolean = false,
        var skipDeprecated: Boolean = false,
        var jdkVersion: Int = 8,
        var languageVersion: String? = null,
        var apiVersion: String? = null,
        var noStdlibLink: Boolean = false,
        var noJdkLink: Boolean = false,
        var suppressedFiles: List<String> = emptyList(),
        var collectInheritedExtensionsFromLibraries: Boolean = true,
        var analysisPlatform: String = "jvm",
        var targets: List<String> = listOf("jvm"),
        var sinceKotlin: String? = null,
        var perPackageOptions: List<PackageOptionsImpl> = emptyList(),
        var externalDocumentationLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
        var sourceLinks: List<SourceLinkDefinitionImpl> = emptyList()
    ) {
        fun build() = PassConfigurationImpl(
            moduleName = moduleName,
            sourceSetName = sourceSetName,
            classpath = classpath,
            sourceRoots = sourceRoots.map { SourceRootImpl(it) },
            dependentSourceRoots = dependentSourceRoots.map { SourceRootImpl(it) },
            dependentSourceSets = dependentSourceSets,
            samples = samples,
            includes = includes,
            includeNonPublic = includeNonPublic,
            includeRootPackage = includeRootPackage,
            reportUndocumented = reportUndocumented,
            skipEmptyPackages = skipEmptyPackages,
            skipDeprecated = skipDeprecated,
            jdkVersion = jdkVersion,
            languageVersion = languageVersion,
            apiVersion = apiVersion,
            noStdlibLink = noStdlibLink,
            noJdkLink = noJdkLink,
            suppressedFiles = suppressedFiles,
            collectInheritedExtensionsFromLibraries = collectInheritedExtensionsFromLibraries,
            analysisPlatform = Platform.fromString(analysisPlatform),
            targets = targets,
            sinceKotlin = sinceKotlin,
            perPackageOptions = perPackageOptions,
            externalDocumentationLinks = externalDocumentationLinks,
            sourceLinks = sourceLinks
        )
    }

    protected val jvmStdlibPath: String? by lazy {
        PathManager.getResourceRoot(Strictfp::class.java, "/kotlin/jvm/Strictfp.class")
    }

    protected val jsStdlibPath: String? by lazy {
        PathManager.getResourceRoot(Any::class.java, "/kotlin/jquery")
    }

    protected val commonStdlibPath: String? by lazy {
        // TODO: feels hacky, find a better way to do it
        ClassLoader.getSystemResource("kotlin/UInt.kotlin_metadata")
            ?.file
            ?.replace("file:", "")
            ?.replaceAfter(".jar", "")
    }
}

data class TestMethods(
    val analysisSetupStage: (Map<SourceSetData, EnvironmentAndFacade>) -> Unit,
    val pluginsSetupStage: (DokkaContext) -> Unit,
    val documentablesCreationStage: (List<DModule>) -> Unit,
    val documentablesFirstTransformationStep: (List<DModule>) -> Unit,
    val documentablesMergingStage: (DModule) -> Unit,
    val documentablesTransformationStage: (DModule) -> Unit,
    val pagesGenerationStage: (ModulePageNode) -> Unit,
    val pagesTransformationStage: (RootPageNode) -> Unit,
    val renderingStage: (RootPageNode, DokkaContext) -> Unit
)