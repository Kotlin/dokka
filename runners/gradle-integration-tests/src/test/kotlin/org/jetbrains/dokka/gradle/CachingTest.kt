package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class CachingTest(val gradleVersion: String, val kotlinVersion: String) : AbstractDokkaGradleTest() {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Gradle {0}, Kotlin {1}")
        fun data(): Iterable<Array<Any>> {
            return arrayListOf(
                arrayOf<Any>("6.3", "1.3.72"),
                arrayOf<Any>("5.6.4", "1.3.72"),
                arrayOf<Any>("5.0", "1.3.72"),
                arrayOf<Any>("6.3", "1.3.61"),
                arrayOf<Any>("5.6.4", "1.3.61"),
                arrayOf<Any>("5.6.4", "1.3.50"),
                arrayOf<Any>("4.10", "1.2.20")
            ).toList()
        }
    }
    private fun prepareTestData(subFolder: String? = null): File {
        val testDataRoot = testDataFolder.resolve("basic")

        val projectDir = if (subFolder == null) testProjectDir.root else testProjectDir.newFolder(subFolder)
        val tmpRoot = projectDir.toPath()

        listOf("src", "classDir", "build.gradle", "settings.gradle").forEach { relativePath ->
            testDataRoot.resolve(relativePath).copy(tmpRoot.resolve(relativePath))
        }

        // clean local cache for each test (shared among subFolder projects)
        tmpRoot.resolve("settings.gradle").toFile().appendText("""

            buildCache {
                local {
                    // Set local build cache directory.
                    directory = "${testProjectDir.root.absolutePath}/build-cache"
                }
            }
        """.trimIndent())

        return projectDir
    }

    private fun run(runner: GradleRunner): TaskOutcome {
        val result = runner.build()
        println(result.output)
        return result.task(":dokka")!!.outcome
    }

    private fun runTwice(first: GradleRunner, second: GradleRunner? = null): List<TaskOutcome> {
        return listOf(run(first), run(second ?: first))
    }

    private fun runner(vararg param: String): GradleRunner {
        return configure(gradleVersion, kotlinVersion, param as Array<String>)
    }

    @Test
    fun `test upToDate`() {
        val p = prepareTestData()
        val runner = runner("dokka")
        val outcomes = runTwice(runner.withProjectDir(p))
        assertEquals(listOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), outcomes)
    }

    @Test
    fun `test cache`() {
        val p = prepareTestData()
        val runner = runner("clean", "dokka", "--build-cache")

        val outcomes = runTwice(runner.withProjectDir(p))

        assertEquals(listOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE), outcomes)
    }

    @Test
    fun `test cache when relocated`() {
        val p1 = prepareTestData("loc1")
        val p2 = prepareTestData("loc2")
        val runner1 = runner("clean", "dokka", "--build-cache")
        val runner2 = runner("clean", "dokka", "--build-cache")

        val outcomes = runTwice(runner1.withProjectDir(p1), runner2.withProjectDir(p2))

        assertEquals(listOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE), outcomes)
    }
}
