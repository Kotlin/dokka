package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class GradleRelocatedCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleCachingIntegrationTest(versions) {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.BASE
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

    private fun runAndAssertOutcome(project: File, expectedOutcome: TaskOutcome) {
        val result = createGradleRunner("clean", "dokkaHtml", "-i", "-s", "-Dorg.gradle.caching.debug=true", "--build-cache")
            .withProjectDir(project)
            .buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)

        File(project, "build/dokka/html").assertHtmlOutputDir()
    }

    private fun projectFolder(index: Int) = File(projectDir.absolutePath + index)
}
