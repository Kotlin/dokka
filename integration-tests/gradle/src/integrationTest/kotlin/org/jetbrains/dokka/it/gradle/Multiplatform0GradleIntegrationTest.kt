package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.*

class Multiplatform0GradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.5.1", "6.1.1"),
            kotlinVersions = listOf("1.3.30", "1.3.72", "1.4-M2-eap-70")
        )
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multiplatform-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner("dokka", "--stacktrace").build()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokka")).outcome)

        val dokkaOutputDir = File(projectDir, "build/dokka")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        dokkaOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLInks(file)
        }
    }
}
