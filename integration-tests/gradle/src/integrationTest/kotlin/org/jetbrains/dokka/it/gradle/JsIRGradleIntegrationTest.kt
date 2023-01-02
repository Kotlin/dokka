package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class JsIRGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.ALL_SUPPORTED
    }

    private val ignoredKotlinVersions = setOf(
        // There were some breaking refactoring changes in kotlin react wrapper libs in 1.4.0 -> 1.5.0,
        // some core react classes were moved from `react-router-dom` to `react` artifacts.
        // Writing an integration test project that would work for both 1.4.0 and 1.5.0 would involve
        // ugly solutions, so these versions are ignored. Not a big loss given they are deprecated as of this moment.
        "1.4.0", "1.4.32"
    )

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-js-ir-0")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .filterNot { it.name == "local.properties" }
            .filterNot { it.name.startsWith("gradlew") }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @Test
    fun execute() {
        if (ignoredKotlinVersions.contains(versions.kotlinVersion)) {
            return
        }

        val reactVersion = TestedVersions.KT_REACT_WRAPPER_MAPPING[versions.kotlinVersion]
            ?: throw IllegalStateException("Unspecified version of react for kotlin " + versions.kotlinVersion)
        val result = createGradleRunner("-Preact_version=$reactVersion", "dokkaHtml", "-i", "-s").buildRelaxed()
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)

        val htmlOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(htmlOutputDir.isDirectory, "Missing html output directory")

        assertTrue(
            htmlOutputDir.allHtmlFiles().count() > 0,
            "Expected html files in html output directory"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
