package org.jetbrains.dokka.it.gradle.kotlin

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.S3Project
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest
import org.jetbrains.dokka.it.gradle.BuildVersions
import org.junit.runners.Parameterized
import java.io.File
import java.net.URL
import kotlin.test.*

class StdlibGradleIntegrationTest(override val versions: BuildVersions) : AbstractGradleIntegrationTest(),
    S3Project {

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val versions = BuildVersions.permutations(
            gradleVersions = listOf("5.6"),
            kotlinVersions = listOf("1.4.10")
        )
    }

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/kotlin-stdlib") }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "stdlib/kotlin-dokka-stdlib")
        templateProjectDir.listFiles().orEmpty()
            .forEach { topLevelFile -> topLevelFile.copyRecursively(File(projectDir, topLevelFile.name)) }

        val pluginDir = File("projects", "stdlib/dokka-samples-transformer-plugin")
        pluginDir.listFiles().orEmpty()
            .forEach { topLevelFile ->
                topLevelFile.copyRecursively(
                    File(
                        projectDir.resolve("dokka-samples-transformer-plugin").also { it.mkdir() }, topLevelFile.name
                    )
                )
            }

        copyAndApplyGitDiff(File("projects", "stdlib/stdlib.diff"))
    }

    @Test
    fun execute() {
        val result = createGradleRunner("callDokka", "-i", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":callDokka")).outcome)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }
    }

    /**
     * Documentation for Enum's synthetic values() and valueOf() functions is only present in source code,
     * but not present in the descriptors. However, Dokka needs to generate documentation for these functions,
     * so it ships with hardcoded kdoc templates.
     *
     * This test exists to make sure documentation for these hardcoded synthetic functions does not change,
     * and fails if it does, indicating that it needs to be updated.
     */
    @Test
    fun shouldAssertEnumDocumentationHasNotChanged() {
        val sourcesLink = "https://raw.githubusercontent.com/JetBrains/kotlin/master/core/builtins/native/kotlin/Enum.kt"
        val sources = URL(sourcesLink).readText()

        val expectedValuesDoc =
        "    /**\n" +
        "     * Returns an array containing the constants of this enum type, in the order they're declared.\n" +
        "     * This method may be used to iterate over the constants.\n" +
        "     * @values\n" +
        "     */"
        check(sources.contains(expectedValuesDoc))

        val expectedValueOfDoc =
        "    /**\n" +
        "     * Returns the enum constant of this type with the specified name. The string must match exactly " +
                "an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)\n" +
        "     * @throws IllegalArgumentException if this enum type has no constant with the specified name\n" +
        "     * @valueOf\n" +
        "     */"
        check(sources.contains(expectedValueOfDoc))
    }
}
