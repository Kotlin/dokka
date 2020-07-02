package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Multiplatform0GradleIntegrationTest : AbstractDefaultVersionsGradleIntegrationTest(
    minGradleVersion = GradleVersion.version("6.0.0")
) {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multiplatform-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    override fun execute(versions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions = versions,
            arguments = arrayOf("dokka", "--stacktrace")
        ).build()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokka")).outcome)

        val dokkaOutputDir = File(projectDir, "build/dokka")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        dokkaOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLInks(file)
        }
    }

}
