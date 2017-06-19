package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

class AndroidAppTest : AbstractDokkaGradleTest() {
    override val pluginClasspath: List<File> = androidPluginClasspathData.toFile().readLines().map { File(it) }

    fun prepareTestData(testDataRootPath: String) {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.resolve("app").copy(tmpRoot.resolve("app"))
        testDataRoot.resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
        testDataRoot.resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))

        val localProperties = testDataRoot.resolve("local.properties")
        if (Files.exists(localProperties)) localProperties.copy(tmpRoot.resolve("local.properties"))
    }


    data class AndroidPluginParams(val pluginVersion: String, val buildToolsVersion: String, val compileSdk: Int) {
        fun asArguments(): List<String> = listOf(
                "-Pabt_plugin_version=$pluginVersion",
                "-Pabt_version=$buildToolsVersion",
                "-Psdk_version=$compileSdk"
        )
    }


    private fun doTest(gradleVersion: String, kotlinVersion: String, androidPluginParams: AndroidPluginParams) {
        prepareTestData("androidApp")

        val result = configure(gradleVersion, kotlinVersion,
                arguments = arrayOf("dokka", "--stacktrace") + androidPluginParams.asArguments())
                .build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:dokka")?.outcome)

        val docsOutput = "app/build/dokka"

        checkOutputStructure("androidApp/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)

        checkExternalLink(docsOutput, "<span class=\"identifier\">Activity</span>",
                """<a href="https://developer.android.com/reference/android/app/Activity.html"><span class="identifier">Activity</span></a>""")
    }

    @Test fun `test kotlin 1_1_2-5 and gradle 4_0 and abt 3_0_0-alpha3`() {
        doTest("4.0", "1.1.2-5", AndroidPluginParams("3.0.0-alpha3", "25.0.0", 25))
    }

    @Test fun `test kotlin 1_1_2 and gradle 3_5 and abt 2_3_0`() {
        doTest("3.5", "1.1.2", AndroidPluginParams("2.3.0", "25.0.0", 24))
    }

    @Test fun `test kotlin 1_0_7 and gradle 3_5 and abt 2_2_3`() {
        doTest("3.5", "1.0.7", AndroidPluginParams("2.2.3", "25.0.0", 24))
    }
}