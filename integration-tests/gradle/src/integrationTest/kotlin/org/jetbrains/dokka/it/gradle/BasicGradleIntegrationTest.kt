package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class BasicGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.5.1", *ifExhaustive("6.4.1", "6.3", "6.2.2", "6.1.1", "6.0"), "5.6.4"),
            kotlinVersions = listOf("1.3.30", *ifExhaustive("1.3.72"), "1.4.0-rc")
        )
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-basic")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner("dokkaHtml", "dokkaJavadoc", "dokkaGfm", "dokkaJekyll", "-i", "-s")
            .buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaJavadoc")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaGfm")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaJekyll")).outcome)

        File(projectDir, "build/dokka/html").assertHtmlOutputDir()
        File(projectDir, "build/dokka/javadoc").assertJavadocOutputDir()
        File(projectDir, "build/dokka/gfm").assertGfmOutputDir()
        File(projectDir, "build/dokka/jekyll").assertJekyllOutputDir()
    }

    private fun File.assertHtmlOutputDir() {
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
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoSuppressedMarker(file)
        }

        assertTrue(
            allHtmlFiles().any { file -> "Basic Project" in file.readText() },
            "Expected configured moduleDisplayName to be present in html"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "integration-tests/gradle/projects/it-basic/" +
                        "src/main/kotlin/it/basic/PublicClass.kt" in file.readText()
            },
            "Expected `PublicClass` source link to GitHub"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "integration-tests/gradle/projects/it-basic/" +
                        "src/main/java/it/basic/java/SampleJavaClass.java" in file.readText()
            },
            "Expected `SampleJavaClass` source link to GitHub"
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
