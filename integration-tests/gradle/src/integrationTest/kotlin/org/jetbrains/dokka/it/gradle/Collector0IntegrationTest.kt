package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class Collector0IntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {
    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = TestedVersions.ALL_SUPPORTED
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-collector-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "moduleA").copyRecursively(File(projectDir, "moduleA"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner(
            ":moduleA:dokkaHtmlCollector",
            ":moduleA:dokkaJavadocCollector",
            ":moduleA:dokkaGfmCollector",
            ":moduleA:dokkaJekyllCollector",
            "-i", "-s"
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaHtmlCollector")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaJavadocCollector")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaGfmCollector")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaJekyllCollector")).outcome)

        File(projectDir, "moduleA/build/dokka/htmlCollector").assertHtmlOutputDir()
        File(projectDir, "moduleA/build/dokka/javadocCollector").assertJavadocOutputDir()
        File(projectDir, "moduleA/build/dokka/gfmCollector").assertGfmOutputDir()
        File(projectDir, "moduleA/build/dokka/jekyllCollector").assertJekyllOutputDir()
    }

    private fun File.assertHtmlOutputDir() {
        assertTrue(isDirectory, "Missing dokka htmlCollector output directory")
        allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        assertTrue(
            allHtmlFiles().any { file -> "moduleB" in file.readText() },
            "Expected moduleB to be present in html"
        )

        assertTrue(
            allHtmlFiles().any { file -> "moduleC" in file.readText() },
            "Expected moduleC to be present in html"
        )
    }

    private fun File.assertJavadocOutputDir() {
        assertTrue(isDirectory, "Missing dokka javadocCollector output directory")
    }

    private fun File.assertJekyllOutputDir() {
        assertTrue(isDirectory, "Missing dokka jekyllCollector output directory")
    }

    private fun File.assertGfmOutputDir() {
        assertTrue(isDirectory, "Missing dokka gfmCollector output directory")
    }
}

