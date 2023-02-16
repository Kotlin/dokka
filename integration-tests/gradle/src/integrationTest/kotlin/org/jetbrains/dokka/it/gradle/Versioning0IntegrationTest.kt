package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.jsoup.Jsoup
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class Versioning0IntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {
    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = TestedVersions.ALL_SUPPORTED
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multimodule-versioning-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "first").copyRecursively(File(projectDir, "first"))
        File(templateProjectDir, "second").copyRecursively(File(projectDir, "second"))
    }

    /**
     * This test runs versioning 3 times to simulate how users might use it in the real word
     *
     * Each version has a separate task that has a different version number from 1.0 to 1.2 and is placed under `builDir/dokkas/<version>`
     *
     * Output is produced in a standard build directory under `build/dokka/htmlMultiModule`
     */
    @Test
    fun execute() {
        val result = createGradleRunner(
            ":dokkaHtmlMultiModuleBaseVersion",
            "-i", "-s"
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtmlMultiModuleBaseVersion")).outcome)
        val outputDir = File(projectDir, "dokkas/1.0")
        assertTrue(outputDir.isDirectory, "Missing dokka output directory")

        val result2 = createGradleRunner(
            "clean",
            ":dokkaHtmlMultiModuleNextVersion",
            "-i", "-s"
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result2.task(":dokkaHtmlMultiModuleNextVersion")).outcome)
        val outputDir2 = File(projectDir, "dokkas/1.1")
        assertTrue(outputDir2.isDirectory, "Missing dokka output directory")

        val result3 = createGradleRunner(
            "clean",
            ":dokkaHtmlMultiModule",
            "-i", "-s"
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result3.task(":dokkaHtmlMultiModule")).outcome)
        val outputDirMultiModule = File(projectDir, "build/dokka/htmlMultiModule")
        assertTrue(outputDirMultiModule.isDirectory, "Missing dokka output directory")

        val version1_0 = outputDirMultiModule.resolve("older").resolve("1.0")
        val version1_1 = outputDirMultiModule.resolve("older").resolve("1.1")

        assertTrue(version1_0.isDirectory, "Assumed to have 1.0 version in older dir")
        assertTrue(version1_1.isDirectory, "Assumed to have 1.1 version in older dir")

        assertFalse(version1_0.resolve("older").exists(), "Subversions should not have older directory")
        assertFalse(version1_1.resolve("older").exists(), "Subversions should not have older directory")

        val parsedIndex = Jsoup.parse(outputDirMultiModule.resolve("index.html").readText())
        val dropdown = parsedIndex.select("dokka-template-command").firstOrNull()
        assertNotNull(dropdown)
        val links = dropdown.select("a")
        assertEquals(3, links.count(), "Expected 3 versions to be in dropdown: 1.0, 1.1 and 1.2")
        assertEquals(
            listOf("1.2" to "index.html", "1.1" to "older/1.1/index.html", "1.0" to "older/1.0/index.html"),
            links.map { it.text() to it.attr("href") }
        )
    }
}