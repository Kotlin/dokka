package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.nio.file.Path
import kotlin.test.assertEquals

class RebuildAfterSourceChangeTest : AbstractDokkaGradleTest() {

    fun prepareTestData(testDataRootPath: String): Pair<Path, Path> {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.resolve("src").copy(tmpRoot.resolve("src"))
        testDataRoot.resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
        testDataRoot.resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))

        return testDataRoot to tmpRoot
    }

    private fun doTest(gradleVersion: String, kotlinVersion: String) {

        val (testDataRoot, tmpRoot) = prepareTestData("sourcesChange")
        val docsOutput = "build/dokka"

        configure(gradleVersion, kotlinVersion, arguments = arrayOf("dokka", "--stacktrace")).build().let { result ->
            println(result.output)

            assertEquals(TaskOutcome.SUCCESS, result.task(":dokka")?.outcome)
        }


        configure(gradleVersion, kotlinVersion, arguments = arrayOf("dokka", "--stacktrace")).build().let { result ->
            println(result.output)

            assertEquals(TaskOutcome.UP_TO_DATE, result.task(":dokka")?.outcome)
        }

        checkOutputStructure("sourcesChange/fileTree.txt", docsOutput)

        testDataRoot.resolve("src1").copy(tmpRoot.resolve("src"))

        configure(gradleVersion, kotlinVersion, arguments = arrayOf("dokka", "--stacktrace")).build().let { result ->
            println(result.output)

            assertEquals(TaskOutcome.SUCCESS, result.task(":dokka")?.outcome)
        }


        checkOutputStructure("sourcesChange/fileTree1.txt", docsOutput)

    }


    @Test
    fun `test kotlin 1_1_2 and gradle 3_5`() {
        doTest("3.5", "1.1.2")
    }

    @Test
    fun `test kotlin 1_0_7 and gradle 2_14_1`() {
        doTest("2.14.1", "1.0.7")
    }

    @Test
    fun `test kotlin 1_1_2 and gradle 4_0`() {
        doTest("4.0", "1.1.2")
    }

    @Test
    fun `test kotlin 1_2_20 and gradle 4_5`() {
        doTest("4.5", "1.2.20")
    }
}