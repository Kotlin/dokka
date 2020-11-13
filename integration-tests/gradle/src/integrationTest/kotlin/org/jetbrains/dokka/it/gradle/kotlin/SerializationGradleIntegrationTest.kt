package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.S3Project
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.*
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

@Ignore // TODO: reenable after https://github.com/Kotlin/kotlinx.serialization/issues/1193 is fixed
class SerializationGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest(),
    S3Project {

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.3"),
            kotlinVersions = listOf("1.4.10")
        )
    }

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/htmlMultiModule") }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "serialization/kotlinx-serialization")
        templateProjectDir.listFiles().orEmpty()
            .forEach { topLevelFile -> topLevelFile.copyRecursively(File(projectDir, topLevelFile.name)) }
        copyAndApplyGitDiff(File("projects", "serialization/serialization.diff"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner(":dokkaHtmlMultiModule", "-i", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtmlMultiModule")).outcome)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
//            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
