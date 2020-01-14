package generator

import com.intellij.util.io.createDirectories
import com.intellij.util.io.exists
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaTestGenerator
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.SourceRootImpl
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import utils.Builders
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class ConfigParams(val name: String, val root: Path, val out: Path)

object TestRunner {

    private val createdDirs: MutableList<File> = mutableListOf()

    fun cleanup() {
        createdDirs.forEach { f -> f.takeIf { it.exists() }?.deleteRecursively() }
        createdDirs.clear()
    }

    fun srcSetFromName(name: String) =
        ClassLoader.getSystemResource("sourceSets/$name").takeIf { it != null }
            ?.let { Paths.get(it.toURI()).toString() }

    fun testFromSourceSets(
        name: String,
        configBuilder: Builders.ConfigBuilder,
        setupTest: (Map<PlatformData, EnvironmentAndFacade>) -> Unit? = {},
        pluginInitTest: (DokkaContext) -> Unit? = {},
        documentablesCreationTest: (List<Module>) -> Unit? = {},
        documentablesMergingTest: (Module) -> Unit? = {},
        documentablesTransformationTest: (Module) -> Unit? = {},
        pagesCreationTest: (ModulePageNode) -> Unit? = {},
        pagesTransformationTest: (ModulePageNode) -> Unit? = {},
        finalTest: (DokkaConfiguration) -> Unit? = {},
        logger: DokkaLogger = DokkaConsoleLogger
    ) {
        val (out, _) = getTestDirs(name)
        DokkaTestGenerator(
            configBuilder(out.toString()),
            logger,
            setupTest,
            pluginInitTest,
            documentablesCreationTest,
            documentablesMergingTest,
            documentablesTransformationTest,
            pagesCreationTest,
            pagesTransformationTest,
            finalTest
        ).generate()
    }

    fun testInline(
        name: String,
        query: String,
        config: (ConfigParams) -> DokkaConfiguration,
        setupTest: (Map<PlatformData, EnvironmentAndFacade>) -> Unit? = {},
        pluginInitTest: (DokkaContext) -> Unit? = {},
        documentablesCreationTest: (List<Module>) -> Unit? = {},
        documentablesMergingTest: (Module) -> Unit? = {},
        documentablesTransformationTest: (Module) -> Unit? = {},
        pagesCreationTest: (ModulePageNode) -> Unit? = {},
        pagesTransformationTest: (ModulePageNode) -> Unit? = {},
        finalTest: (DokkaConfiguration) -> Unit? = {},
        logger: DokkaLogger = DokkaConsoleLogger
    ) {

        val (root, out) = getTestDirs(name)
        query.toFileMap().materializeFiles(root)
        DokkaTestGenerator(
            config(ConfigParams(name, root, out)),
            logger,
            setupTest,
            pluginInitTest,
            documentablesCreationTest,
            documentablesMergingTest,
            documentablesTransformationTest,
            pagesCreationTest,
            pagesTransformationTest,
            finalTest
        ).generate()
    }

    fun generatePassesForPlatforms(
        testName: String,
        platforms: List<String>,
        passBuilder: Builders.PassBuilder
    ): List<Builders.PassBuilder> {
        fun File.nameLower() = this.name.toLowerCase()
        fun File.isCommon() = this.nameLower().contains("common")
        fun File.getPlatforms(platforms: List<String>) =
            platforms.filter { this.nameLower().contains(it.toLowerCase()) }


        val testSrcDirs =
            srcSetFromName(testName)?.let { fName ->
                File(fName).listFiles()?.filter { it.isDirectory }
            } ?: emptyList()

        val dirs =
            platforms.associateWith { platform ->
                testSrcDirs.filter { file ->
                    platform == "common" && file.isCommon() && file.getPlatforms(platforms).size == 1 ||
                            platform != "common" && file.nameLower().contains(platform.toLowerCase())
                }.map { it.path }
            }.filter { (_, value) -> value.isNotEmpty() }

        return dirs.map { (platform, srcs) ->
            passBuilder.copy(moduleName = "$testName-$platform", analysisPlatform = platform, sourceRoots = srcs)
        }
    }

    fun getTestDirs(name: String) =
        Files.createTempDirectory(name).also { it.takeIf { it.exists() }?.deleteContents() }
            .let { it to it.resolve("out").also { it.createDirectories() } }
            .also { createdDirs += it.first.toFile() }

    fun String.toFileMap(): Map<String, String> = this.replace("\r\n", "\n")
        .split("\n/")
        .map {fileString ->
            fileString.split("\n", limit = 2)
                .let {
                    it.first().trim().removePrefix("/") to it.last().trim()
                }
        }
        .toMap()

    fun Map<String, String>.materializeFiles(
        root: Path = Paths.get("."),
        charset: Charset = Charset.forName("utf-8")
    ) = this.map { (path, content) ->
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.write(file, content.toByteArray(charset))
    }

    fun File.deleteContents() {
        this.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun Path.deleteContents() {
        this.toFile().deleteContents()
    }

}