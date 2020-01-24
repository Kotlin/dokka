package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class MultiplatformProjectTest : AbstractDokkaGradleTest() {

    fun prepareTestData(testDataRootPath: String) {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.apply {
            resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
            resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))
            resolve("src").copy(tmpRoot.resolve("src"))
        }
    }

    private fun doTest(gradleVersion: String, kotlinVersion: String) {
        val kotlinGradlePlugin = pluginClasspathData.resolveSibling("kotlin-gradle.txt").toFile().readLines().map { File(it) }
        prepareTestData("multiplatformProject")

        // Remove withDebug(false) when https://github.com/gradle/gradle/issues/6862 is solved
        val result = configure(gradleVersion, kotlinVersion, arguments = arrayOf("dokka", "--stacktrace"))
            .withDebug(false)
            .withPluginClasspath(pluginClasspath.union(kotlinGradlePlugin))
            .build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":dokka")?.outcome)

        val docsOutput = "build/dokka"

        checkOutputStructure("multiplatformProject/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)
    }

    @Test fun `test kotlin 1_3_30 and gradle 4_9`() {
        doTest("4.9", "1.3.30")
    }

    @Test fun `test kotlin 1_3_40 and gradle 4_10_3`() {
        doTest("4.10.3", "1.3.40")
    }

    @Test fun `test kotlin 1_3_40 and gradle 5_6_1`() {
        doTest("5.6.1", "1.3.50")
    }
}