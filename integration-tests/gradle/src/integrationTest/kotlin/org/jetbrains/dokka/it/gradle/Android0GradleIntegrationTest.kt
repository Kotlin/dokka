package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.test.assumeAndroidSdkInstalled
import org.junit.runners.Parameterized.Parameters
import java.io.File
import kotlin.test.*

class Android0GradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest() {

    companion object {
        @get:JvmStatic
        @get:Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("6.6", *ifExhaustive("5.6.4")),
            kotlinVersions = listOf("1.4.0", *ifExhaustive("1.3.72")),
            androidGradlePluginVersions = listOf("3.5.3", *ifExhaustive("3.6.3"))
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.6", *ifExhaustive("6.1.1")),
            kotlinVersions = listOf("1.4.0", *ifExhaustive("1.3.72")),
            androidGradlePluginVersions = listOf("4.0.0")
        ) + BuildVersions.permutations(
            gradleVersions = listOf("6.6"),
            kotlinVersions = listOf("1.4.0", *ifExhaustive("1.3.72")),
            androidGradlePluginVersions = listOf("4.1.0-beta02")
        )
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
