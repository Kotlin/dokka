package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.assertEquals

abstract class AbstractAndroidAppTest(val testDataRootPath: String) : AbstractDokkaAndroidGradleTest() {

    fun prepareTestData() {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.resolve("app").copy(tmpRoot.resolve("app"))
        testDataRoot.resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
        testDataRoot.resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))

        androidLocalProperties?.copy(tmpRoot.resolve("local.properties"))
    }


    data class AndroidPluginParams(val pluginVersion: String, val buildToolsVersion: String, val compileSdk: Int) {
        fun asArguments(): List<String> = listOf(
                "-Pabt_plugin_version=$pluginVersion",
                "-Pabt_version=$buildToolsVersion",
                "-Psdk_version=$compileSdk"
        )
    }


    protected fun doTest(gradleVersion: String, kotlinVersion: String, androidPluginParams: AndroidPluginParams) {
        prepareTestData()

        val result = configure(gradleVersion, kotlinVersion,
                arguments = arrayOf("dokka", "--stacktrace") + androidPluginParams.asArguments())
                .build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:dokka")?.outcome)

        val docsOutput = "app/build/dokka"

        checkOutputStructure("$testDataRootPath/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)

        checkExternalLink(docsOutput, "<span class=\"identifier\">Activity</span>",
                """<a href="https://developer.android.com/reference/android/app/Activity.html"><span class="identifier">Activity</span></a>""")
    }

}