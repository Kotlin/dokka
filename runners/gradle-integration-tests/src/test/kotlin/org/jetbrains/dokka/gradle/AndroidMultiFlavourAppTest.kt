package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.gradle.AbstractAndroidAppTest.AndroidPluginParams
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class AndroidMultiFlavourAppTest : AbstractDokkaAndroidGradleTest() {

    fun prepareTestData(testDataRootPath: String) {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.resolve("app").copy(tmpRoot.resolve("app"))
        testDataRoot.resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
        testDataRoot.resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))

        androidLocalProperties?.copy(tmpRoot.resolve("local.properties"))
    }

    private fun doTest(gradleVersion: String, kotlinVersion: String, androidPluginParams: AndroidPluginParams) {
        prepareTestData("androidMultiFlavourApp")

        val result = configure(gradleVersion, kotlinVersion,
                arguments = arrayOf("dokka", "dokkaFullFlavourOnly", "--stacktrace") + androidPluginParams.asArguments())
                .build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:dokka")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:dokkaFullFlavourOnly")?.outcome)

        val docsOutput = "app/build/dokka"

        checkOutputStructure("androidMultiFlavourApp/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)

        checkExternalLink(docsOutput, "<span class=\"identifier\">Activity</span>",
                """<a href="https://developer.android.com/reference/android/app/Activity.html"><span class="identifier">Activity</span></a>""")
    }

    // TODO FIXME
    @Ignore
    @Test
    fun `test kotlin 1_1_2-5 and gradle 4_0 and abt 3_0_0-alpha3`() {
        doTest("4.0", "1.1.2-5", AndroidPluginParams("3.0.0-alpha3", "25.0.2", 25))
    }

    // TODO FIXME
    @Ignore
    @Test
    fun `test kotlin 1_1_2 and gradle 3_5 and abt 2_3_0`() {
        doTest("3.5", "1.1.2", AndroidPluginParams("2.3.0", "25.0.0", 24))
    }

    // TODO FIXME
    @Ignore
    @Test
    fun `test kotlin 1_0_7 and gradle 2_14_1 and abt 2_2_3`() {
        doTest("2.14.1", "1.0.7", AndroidPluginParams("2.2.3", "25.0.0", 24))
    }

    // TODO FIXME
    @Ignore
    @Test
    fun `test kotlin 1_2_20 and gradle 4_5 and abt 3_0_1`() {
        doTest("4.5", "1.2.20", AndroidPluginParams("3.0.1", "27.0.0", 27))
    }

}