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

    @Test
    fun `test gradle 6_5_1 kotlin 1_3_72`() {
        doTest(gradleVersion = "6.5.1", kotlinVersion = "1.3.72")
    }

    @Test
    fun  `test gradle 6_5_0 kotlin 1_3_72`() {
        doTest(gradleVersion = "6.5.0", kotlinVersion = "1.3.72")
    }

    @Test
    fun  `test gradle 5_6_4 kotlin 1_3_72`() {
        doTest(gradleVersion = "5.6.4", kotlinVersion = "1.3.72")
    }

    @Test
    fun  `test gradle 5_6_4 kotlin 1_3_30`() {
        doTest(gradleVersion = "5.6.4", kotlinVersion = "1.3.30")
    }

}
