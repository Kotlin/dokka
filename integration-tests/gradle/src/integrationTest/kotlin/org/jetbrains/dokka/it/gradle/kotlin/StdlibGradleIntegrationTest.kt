package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.S3Project
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.*
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class StdlibGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest(),
    S3Project {

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("5.6"),
            kotlinVersions = listOf("1.4.10")
        )
    }

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/kotlin-stdlib") }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "stdlib/kotlin-dokka-stdlib")
        templateProjectDir.listFiles().orEmpty()
            .forEach { topLevelFile -> topLevelFile.copyRecursively(File(projectDir, topLevelFile.name)) }

        copyAndApplyGitDiff(File("projects", "stdlib/stdlib.diff"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner("callDokka", "-i", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":callDokka")).outcome)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
//            assertNoUnresolvedLinks(file)
//            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }
    }
}
