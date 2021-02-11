package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.S3Project
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest
import org.jetbrains.dokka.it.gradle.BuildVersions
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class KtorGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest(),
    S3Project {

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/htmlMultiModule") }

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.8.2"),
            kotlinVersions = listOf("1.4.21")
        )
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "ktor/ktor")
        templateProjectDir.listFiles().orEmpty()
            .forEach { topLevelFile -> topLevelFile.copyRecursively(File(projectDir, topLevelFile.name)) }
        copyAndApplyGitDiff(File("projects", "ktor/ktor.diff"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner("assemble", ":dokkaHtmlMultiModule", "-i", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtmlMultiModule")).outcome)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
//            assertContainsNoErrorClass(file)
//            assertNoUnresolvedLinks(file)
//            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }
    }
}