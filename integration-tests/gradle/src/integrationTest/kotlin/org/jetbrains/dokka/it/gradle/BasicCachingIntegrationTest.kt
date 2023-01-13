package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class BasicCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleCachingIntegrationTest(versions) {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.BASE
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

    @Test
    fun localDirectoryPointingToRoot() {
        fun String.findAndReplace(oldValue: String, newValue: String): String {
            assertTrue(oldValue in this, "Expected to replace '$oldValue'")
            return replace(oldValue, newValue)
        }
        val projectKts = projectDir.resolve("build.gradle.kts")
        val projectKtsText = projectKts.readText()
            .findAndReplace("localDirectory.set(file(\"src/main\"))", "localDirectory.set(projectDir)")
            .findAndReplace("integration-tests/gradle/projects/it-basic/src/main", "integration-tests/gradle/projects/it-basic")
        projectKts.writeText(projectKtsText)

        runAndAssertOutcome(TaskOutcome.SUCCESS)
        projectDir.resolve("unrelated.txt").writeText("modified")
        // despite projectDir is used as an input in localDirectory, changing its contents shouldn't invalidate the cache
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
