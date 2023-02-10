package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class MultiModule0IntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {
    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = TestedVersions.ALL_SUPPORTED
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multimodule-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "moduleA").copyRecursively(File(projectDir, "moduleA"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner(
            ":moduleA:dokkaHtmlMultiModule",
            ":moduleA:dokkaGfmMultiModule",
            ":moduleA:dokkaJekyllMultiModule",
            "-i", "-s"
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaHtmlMultiModule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaGfmMultiModule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaJekyllMultiModule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaHtmlPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaHtmlPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaGfmPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaGfmPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaJekyllPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaJekyllPartial")).outcome)


        val outputDir = File(projectDir, "moduleA/build/dokka/htmlMultiModule")
        assertTrue(outputDir.isDirectory, "Missing dokka output directory")

        assertTrue(
            outputDir.allHtmlFiles().any(),
            "Expected at least one html file being generated"
        )

        outputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }

        val modulesFile = File(outputDir, "index.html")
        assertTrue(modulesFile.isFile, "Missing index.html file")

        val modulesFileText = modulesFile.readText()
        assertTrue(
            "moduleB" in modulesFileText,
            "Expected moduleB being mentioned in -modules.html"
        )
        assertTrue(
            "moduleC" in modulesFileText,
            "Expected moduleC being mentioned in -modules.html"
        )

        val gfmOutputDir = File(projectDir, "moduleA/build/dokka/gfmMultiModule")
        assertTrue(gfmOutputDir.isDirectory, "Missing dokka GFM output directory")

        assertTrue(
            gfmOutputDir.allGfmFiles().any(),
            "Expected at least one md file being generated"
        )

        gfmOutputDir.allGfmFiles().forEach { file ->
            assertFalse("GfmCommand" in file.readText())
        }
    }
}
