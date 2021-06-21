package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class BasicCachingIntegrationTest(override val versions: BuildVersions) : AbstractGradleCachingIntegrationTest(versions) {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("7.0", *ifExhaustive("6.6", "6.1.1")),
            kotlinVersions = listOf("1.3.30", *ifExhaustive("1.3.72", "1.4.32"), "1.5.0")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("5.6.4", "6.0"),
            kotlinVersions = listOf("1.3.30", *ifExhaustive("1.4.32"))
        )
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
