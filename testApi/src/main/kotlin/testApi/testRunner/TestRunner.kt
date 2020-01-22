package testApi.testRunner

import org.jetbrains.dokka.*
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
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
        DokkaTestGenerator(newConfiguration, logger, testMethods).generate()
    }

    protected fun testInline(
        query: String,
        configuration: DokkaConfigurationImpl,
        cleanupOutput: Boolean = true,
        block: TestBuilder.() -> Unit
    ) {
        val testMethods = TestBuilder().apply(block).build()
        val testDirPath = getTempDir(cleanupOutput).root.toPath()
        val fileMap = query.toFileMap()
        fileMap.materializeFiles(testDirPath.toAbsolutePath())
        if (!cleanupOutput)
            logger.info("Output generated under: ${testDirPath.toAbsolutePath()}")
        val newConfiguration =
            configuration.copy(
                outputDir = testDirPath.toAbsolutePath().toString(),
                passesConfigurations = configuration.passesConfigurations
                    .map { it.copy(sourceRoots = it.sourceRoots.map { it.copy(path = "${testDirPath.toAbsolutePath()}/${it.path}") }) }
            )
        DokkaTestGenerator(newConfiguration, logger, testMethods).generate()
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
        var analysisSetupStage: (Map<PlatformData, EnvironmentAndFacade>) -> Unit = {}
        var pluginsSetupStage: (DokkaContext) -> Unit = {}
        var documentablesCreationStage: (List<Module>) -> Unit = {}
        var documentablesMergingStage: (Module) -> Unit = {}
        var documentablesTransformationStage: (Module) -> Unit = {}
        var pagesGenerationStage: (ModulePageNode) -> Unit = {}
        var pagesTransformationStage: (ModulePageNode) -> Unit = {}

        fun build() = TestMethods(
            analysisSetupStage,
            pluginsSetupStage,
            documentablesCreationStage,
            documentablesMergingStage,
            documentablesTransformationStage,
            pagesGenerationStage,
            pagesTransformationStage
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
        private val passesConfigurations = mutableListOf<PassConfigurationImpl>()

        fun build() = DokkaConfigurationImpl(
            outputDir = outputDir,
            format = format,
            generateIndexPages = generateIndexPages,
            cacheRoot = cacheRoot,
            impliedPlatforms = emptyList(),
            passesConfigurations = passesConfigurations,
            pluginsClasspath = pluginsClasspath
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
        var classpath: List<String> = emptyList(),
        var sourceRoots: List<String> = emptyList(),
        var samples: List<String> = emptyList(),
        var includes: List<String> = emptyList(),
        var includeNonPublic: Boolean = true,
        var includeRootPackage: Boolean = true,
        var reportUndocumented: Boolean = false,
        var skipEmptyPackages: Boolean = false,
        var skipDeprecated: Boolean = false,
        var jdkVersion: Int = 6,
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
            classpath = classpath,
            sourceRoots = sourceRoots.map { SourceRootImpl(it) },
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
}

data class TestMethods(
    val analysisSetupStage: (Map<PlatformData, EnvironmentAndFacade>) -> Unit,
    val pluginsSetupStage: (DokkaContext) -> Unit,
    val documentablesCreationStage: (List<Module>) -> Unit,
    val documentablesMergingStage: (Module) -> Unit,
    val documentablesTransformationStage: (Module) -> Unit,
    val pagesGenerationStage: (ModulePageNode) -> Unit,
    val pagesTransformationStage: (ModulePageNode) -> Unit
)