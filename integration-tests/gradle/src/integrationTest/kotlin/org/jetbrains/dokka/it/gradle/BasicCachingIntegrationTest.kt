package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class BasicCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleCachingIntegrationTest(versions) {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.JVM
    }

    @BeforeTest
    fun setupProjectFiles(){
        setupProject(projectDir)
    }

    @Test
    fun execute() {
        runAndAssertOutcome(TaskOutcome.SUCCESS)
        runAndAssertOutcome(TaskOutcome.FROM_CACHE)
    }

    private fun runAndAssertOutcome(expectedOutcome: TaskOutcome) {
        val result = createGradleRunner(
            "clean",
            "dokkaHtml",
            "-i",
            "-s",
            "-Dorg.gradle.caching.debug=true",
            "--build-cache"
        ).buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)

        File(projectDir, "build/dokka/html").assertHtmlOutputDir()
    }
}
