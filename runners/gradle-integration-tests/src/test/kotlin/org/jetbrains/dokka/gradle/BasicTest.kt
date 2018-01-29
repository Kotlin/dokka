package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import kotlin.test.assertEquals

class BasicTest : AbstractDokkaGradleTest() {

    fun prepareTestData(testDataRootPath: String) {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.resolve("src").copy(tmpRoot.resolve("src"))
        testDataRoot.resolve("classDir").copy(tmpRoot.resolve("classDir"))
        testDataRoot.resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
        testDataRoot.resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))
    }

    private fun doTest(gradleVersion: String, kotlinVersion: String) {

        prepareTestData("basic")

        val result = configure(gradleVersion, kotlinVersion, arguments = arrayOf("dokka", "--stacktrace")).build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":dokka")?.outcome)

        val docsOutput = "build/dokka"

        checkOutputStructure("basic/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)

        checkExternalLink(docsOutput, "<span class=\"identifier\">String</span>",
                """<a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html"><span class="identifier">String</span></a>""")
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

    @Test fun `test kotlin 1_2_20 and gradle 4_5`() {
        doTest("4.5", "1.2.20")
    }
}