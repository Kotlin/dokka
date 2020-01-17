package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import kotlin.test.assertEquals

class AndroidLibDependsOnJavaLibTest: AbstractDokkaAndroidGradleTest() {

    private val testDataRootPath = "androidLibDependsOnJavaLib"

    private fun prepareTestData() {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.copy(tmpRoot)

        androidLocalProperties?.copy(tmpRoot.resolve("local.properties"))
    }


    private fun doTest(gradleVersion: String, kotlinVersion: String, androidPluginParams: AbstractAndroidAppTest.AndroidPluginParams) {
        prepareTestData()

        val result = configure(gradleVersion, kotlinVersion,
                arguments = arrayOf("dokka", "--stacktrace") + androidPluginParams.asArguments())
                .build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":lib:dokka")?.outcome)

        val docsOutput = "lib/build/dokka"

        checkOutputStructure("$testDataRootPath/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)

        checkExternalLink(docsOutput, "<span class=\"identifier\">LibClz</span>",
                """<a href="https://example.com/example/jlib/LibClz.html"><span class="identifier">LibClz</span></a>""")
    }


    @Test
    fun `test kotlin 1_2_20 and gradle 4_5 and abt 3_0_1`() {
        doTest("4.5", "1.2.20", AbstractAndroidAppTest.AndroidPluginParams("3.0.1", "27.0.0", 27))
    }
}