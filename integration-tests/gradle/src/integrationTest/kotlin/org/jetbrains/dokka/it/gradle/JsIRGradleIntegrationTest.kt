package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class JsIRGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.BASE
    }

    val reactVersionArg = mapOf(
        "1.5.0" to "-Pdokka_it_react_kotlin_version=5.2.0-pre.202-kotlin-1.5.0",
        "1.5.30" to "-Pdokka_it_react_kotlin_version=5.2.0-pre.246-kotlin-1.5.30",
        "1.5.31" to "-Pdokka_it_react_kotlin_version=5.2.0-pre.251-kotlin-1.5.31"
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
        val reactPropertyArg = reactVersionArg[versions.kotlinVersion]
            ?: throw IllegalStateException("Unspecified version of react for kotlin " + versions.kotlinVersion)
        val result = createGradleRunner(reactPropertyArg, "dokkaHtml", "-i", "-s").buildRelaxed()
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