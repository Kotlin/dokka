package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class BasicCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.5.1", "6.4.1", "6.3", "6.2.2", "6.1.1", "6.0"),
            kotlinVersions = listOf("1.3.30", "1.3.72", "1.4-M3")
        )
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-basic")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))

        // clean local cache for each test
        projectDir.toPath().resolve("settings.gradle.kts").toFile().appendText(
            """
            buildCache {
                local {
                    // Set local build cache directory.
                    directory = File("${projectDir.absolutePath}", "build-cache")
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun execute() {
        runAndAssertOutcome(TaskOutcome.SUCCESS)
        runAndAssertOutcome(TaskOutcome.FROM_CACHE)
    }

    private fun runAndAssertOutcome(expectedOutcome: TaskOutcome) {
        val result = createGradleRunner(
            "clean",
            "dokkaHtml",
            "dokkaJavadoc",
            "dokkaGfm",
            "dokkaJekyll",
            "-i",
            "-s",
            "--build-cache"
        ).buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaJavadoc")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaGfm")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaJekyll")).outcome)

        File(projectDir, "build/dokka/html").assertKdocOutputDir()
        File(projectDir, "build/dokka/javadoc").assertJavadocOutputDir()
        File(projectDir, "build/dokka/gfm").assertGfmOutputDir()
        File(projectDir, "build/dokka/jekyll").assertJekyllOutputDir()
    }

    private fun File.assertKdocOutputDir() {
        assertTrue(isDirectory, "Missing dokka html output directory")

        val imagesDir = File(this, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(this, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")

        val stylesDir = File(this, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")

        val navigationHtml = File(this, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")

        val moduleOutputDir = File(this, "-basic -project")
        assertTrue(moduleOutputDir.isDirectory, "Missing module directory")

        val moduleIndexHtml = File(moduleOutputDir, "index.html")
        assertTrue(moduleIndexHtml.isFile, "Missing module index.html")

        val modulePackageDir = File(moduleOutputDir, "it.basic")
        assertTrue(modulePackageDir.isDirectory, "Missing it.basic package directory")

        val modulePackageIndexHtml = File(modulePackageDir, "index.html")
        assertTrue(modulePackageIndexHtml.isFile, "Missing module package index.html")

        val moduleJavaPackageDir = File(moduleOutputDir, "it.basic.java")
        assertTrue(moduleJavaPackageDir.isDirectory, "Missing it.basic.java package directory")

        allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLInks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
        }

        assertTrue(
            allHtmlFiles().any { file -> "Basic Project" in file.readText() },
            "Expected configured moduleDisplayName to be present in html"
        )
    }

    private fun File.assertJavadocOutputDir() {
        assertTrue(isDirectory, "Missing dokka javadoc output directory")
    }

    private fun File.assertGfmOutputDir() {
        assertTrue(isDirectory, "Missing dokka gfm output directory")
    }

    private fun File.assertJekyllOutputDir() {
        assertTrue(isDirectory, "Missing dokka jekyll output directory")
    }
}
