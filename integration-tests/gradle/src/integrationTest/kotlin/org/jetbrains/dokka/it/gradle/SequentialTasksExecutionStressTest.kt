package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

/**
 *  Creates 100 tasks for the test project and runs them sequentially under low memory settings.
 *
 *  If the test passes, it's likely there are no noticeable memory leaks.
 *  If it fails, it's likely that memory is leaking somewhere.
 */
class SequentialTasksExecutionStressTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions =  listOf(TestedVersions.LATEST)
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-sequential-tasks-execution-stress")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner(
            "runTasks",
            "--info",
            "--stacktrace",
            "-Ptask_number=100",
            jvmArgs = listOf("-Xmx1G", "-XX:MaxMetaspaceSize=350m")
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":runTasks")).outcome)
    }
}
