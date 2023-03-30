package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.*
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*
import kotlin.test.Test

class Android0GradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = TestedVersions.ANDROID

        /**
         * Indicating whether or not the current machine executing the test is a CI
         */
        private val isCI: Boolean get() = System.getenv("CI") == "true"

        private val isAndroidSdkInstalled: Boolean = System.getenv("ANDROID_SDK_ROOT") != null ||
                System.getenv("ANDROID_HOME") != null

        fun assumeAndroidSdkInstalled() {
            if (isCI) return
            Assume.assumeTrue(isAndroidSdkInstalled)
        }

    }

    @BeforeTest
    fun prepareProjectFiles() {
        assumeAndroidSdkInstalled()
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
        val result = createGradleRunner("dokkaHtml", "-i", "-s").buildRelaxed()
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)

        val htmlOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(htmlOutputDir.isDirectory, "Missing html output directory")

        assertTrue(
            htmlOutputDir.allHtmlFiles().count() > 0,
            "Expected html files in html output directory"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file, knownUnresolvedDRIs)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        assertTrue(
            htmlOutputDir.allHtmlFiles().any { file ->
                "https://developer.android.com/reference/kotlin/android/content/Context.html" in file.readText()
            }, "Expected link to developer.android.com"
        )

        assertTrue(
            htmlOutputDir.allHtmlFiles().any { file ->
                "https://developer.android.com/reference/kotlin/androidx/appcompat/app/AppCompatActivity.html" in
                        file.readText()
            }, "Expected link to developer.android.com/.../androidx/"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
        }
    }

    // TODO: remove this list when https://github.com/Kotlin/dokka/issues/1306 is closed
    private val knownUnresolvedDRIs = setOf(
        "it.android/IntegrationTestActivity/findViewById/#kotlin.Int/PointingToGenericParameters(0)/",
        "it.android/IntegrationTestActivity/getExtraData/#java.lang.Class[TypeParam(bounds=[androidx.core.app.ComponentActivity.ExtraData])]/PointingToGenericParameters(0)/",
        "it.android/IntegrationTestActivity/getSystemService/#java.lang.Class[TypeParam(bounds=[kotlin.Any])]/PointingToGenericParameters(0)/",
        "it.android/IntegrationTestActivity/requireViewById/#kotlin.Int/PointingToGenericParameters(0)/"
    )
}
