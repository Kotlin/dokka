package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class BasicTest : AbstractBasicDokkaGradleTest() {

    fun prepareTestData(testDataRootPath: String, buildfile: String = "default.gradle") {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.resolve("src").copy(tmpRoot.resolve("src"))
        testDataRoot.resolve(buildfile).copy(tmpRoot.resolve("build.gradle"))
    }

    private fun doTest(gradleVersion: String, kotlinVersion: String) {

        prepareTestData("basic")

        testProjectDir.root.printStructure()
        val result = configure(gradleVersion, kotlinVersion, arguments = arrayOf("dokka", "--stacktrace")).build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":dokka")?.outcome)

        File(testProjectDir.root, "build/dokka").printStructure()

        val docsOutput = "build/dokka/${testProjectDir.root.name}"

        checkOutputStructure("basic/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)

        checkNoUnresolvedLinks(docsOutput)
    }

    @Test fun `test kotlin 1_1_2 and gradle 3_5`() {
        doTest("3.5", "1.1.2")
    }

    @Test fun `test kotlin 1_0_7 and gradle 2_14_1`() {
        doTest("2.14.1", "1.0.7")
    }

    @Test fun `test kotlin 1_1_2 and gradle 4_0`() {
        doTest("4.0", "1.1.2")
    }

}