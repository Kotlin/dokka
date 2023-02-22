package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class BasicCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleCachingIntegrationTest(versions) {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.ALL_SUPPORTED
    }

    @BeforeTest
    fun setupProjectFiles(){
        setupProject(projectDir)
    }

    @Test
    fun execute() {
        runAndAssertOutcomeAndContents(TaskOutcome.SUCCESS)
        runAndAssertOutcomeAndContents(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun localDirectoryPointingToRoot() {
        fun String.findAndReplace(oldValue: String, newValue: String): String {
            assertTrue(oldValue in this, "Expected to replace '$oldValue'")
            return replace(oldValue, newValue)
        }
        val projectKts = projectDir.resolve("build.gradle.kts")

        projectKts.readText()
            .findAndReplace("localDirectory.set(file(\"src/main\"))", "localDirectory.set(projectDir)")
            .findAndReplace("integration-tests/gradle/projects/it-basic/src/main", "integration-tests/gradle/projects/it-basic")
            .also { projectKts.writeText(it) }

        runAndAssertOutcomeAndContents(TaskOutcome.SUCCESS)
        projectDir.resolve("unrelated.txt").writeText("modified")
        // despite projectDir is used as an input in localDirectory, changing its contents shouldn't invalidate the cache
        runAndAssertOutcomeAndContents(TaskOutcome.FROM_CACHE)

        projectKts.readText()
            .findAndReplace("localDirectory.set(projectDir)", "localDirectory.set(file(\"src\"))")
            .also { projectKts.writeText(it) }
        // changing localDirectory path invalidates cached task results
        runAndAssertOutcome(TaskOutcome.SUCCESS)
    }


    private fun runAndAssertOutcomeAndContents(expectedOutcome: TaskOutcome) {
        runAndAssertOutcome(expectedOutcome)
        File(projectDir, "build/dokka/html").assertHtmlOutputDir()
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
    }
}
