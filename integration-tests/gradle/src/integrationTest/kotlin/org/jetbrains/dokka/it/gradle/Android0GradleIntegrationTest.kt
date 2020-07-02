package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Android0GradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.5.1", "5.6.4"),
            kotlinVersions = listOf("1.3.72", "1.4-M2-eap-70"),
            androidGradlePluginVersions = listOf("3.5.3", "3.6.3")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.5.1","6.1.1"),
            kotlinVersions = listOf("1.3.72", "1.4-M2-eap-70"),
            androidGradlePluginVersions = listOf("4.0.0")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.5.1"),
            kotlinVersions = listOf("1.3.72", "1.4-M2-eap-70"),
            androidGradlePluginVersions = listOf("4.1.0-beta02")
        )
    }

    @BeforeTest
    fun assumeAndroidInstallation() {
        val isCI = System.getenv("CI") != null
        if (isCI) {
            return
        }

        val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")
        val androidHome = System.getenv("ANDROID_HOME")

        val isAndroidSdkInstalled = androidSdkRoot != null || androidHome != null

        Assume.assumeTrue(
            "Missing ANDROID_SDK_ROOT",
            isAndroidSdkInstalled
        )
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-android-0")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .filterNot { it.name == "local.properties" }
            .filterNot { it.name.startsWith("gradlew") }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner("dokka", "--stacktrace").build()
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokka")).outcome)
    }

}
