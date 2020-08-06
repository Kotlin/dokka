package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class GradleRelocatedCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

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
        setupProject(projectFolder(1))
        setupProject(projectFolder(2))
    }

    @Test
    fun execute() {
        runAndAssertOutcome(projectFolder(1), TaskOutcome.SUCCESS)
        runAndAssertOutcome(projectFolder(2), TaskOutcome.FROM_CACHE)
    }

    private fun setupProject(project: File) {
        val templateDir = File("projects", "it-basic")
        project.mkdir()
        templateDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(project, topLevelFile.name)) }

        File(templateDir, "src").copyRecursively(File(project, "src"))

        // clean local cache for each test (shared among projects)
        project.toPath().resolve("settings.gradle.kts").toFile().appendText(
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

    private fun runAndAssertOutcome(project: File, expectedOutcome: TaskOutcome) {
        val result = createGradleRunner("clean", "dokkaHtml", "dokkaJavadoc", "dokkaGfm", "dokkaJekyll", "-i", "-s", "--build-cache")
            .withProjectDir(project)
            .buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaJavadoc")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaGfm")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaJekyll")).outcome)

        File(project, "build/dokka/html").assertKdocOutputDir()
        File(project, "build/dokka/javadoc").assertJavadocOutputDir()
        File(project, "build/dokka/gfm").assertGfmOutputDir()
        File(project, "build/dokka/jekyll").assertJekyllOutputDir()
    }

    private fun projectFolder(index: Int) = File(projectDir.absolutePath + index)

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
