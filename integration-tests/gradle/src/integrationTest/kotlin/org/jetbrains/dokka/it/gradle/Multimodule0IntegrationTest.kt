package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class Multimodule0IntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {
    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.5.1", "6.1.1"),
            kotlinVersions = listOf("1.4.0-rc")
        )
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
            ":moduleA:dokkaHtmlMultimodule",
            ":moduleA:dokkaGfmMultimodule",
            ":moduleA:dokkaJekyllMultimodule",
            "-i", "-s"
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaHtmlMultimodule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaGfmMultimodule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaJekyllMultimodule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaHtml")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaHtml")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaGfm")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaGfm")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaJekyll")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaJekyll")).outcome)


        val outputDir = File(projectDir, "moduleA/build/dokka/htmlMultimodule")
        assertTrue(outputDir.isDirectory, "Missing dokka output directory")

        assertTrue(
            outputDir.allHtmlFiles().any(),
            "Expected at least one html file being generated"
        )

        outputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
        }

        val modulesFile = File(outputDir, "-modules.html")
        assertTrue(modulesFile.isFile, "Missing -modules.html file")

        val modulesFileText = modulesFile.readText()
        assertTrue(
            "moduleB" in modulesFileText,
            "Expected moduleB being mentioned in -modules.html"
        )
        assertTrue(
            "moduleC" in modulesFileText,
            "Expected moduleC being mentioned in -modules.html"
        )

    }
}
