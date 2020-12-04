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

    @Test fun `test kotlin 1_4_20 and gradle 6_7_1`() {
        doTest("6.7.1", "1.4.20")
    }

    @Test fun `test kotlin 1_3_70 and gradle 6_0`() {
        doTest("6.0", "1.3.70")
    }

    @Test fun `test kotlin 1_4_0 and gradle 6_0`() {
        doTest("6.0", "1.4.0")
    }
}
