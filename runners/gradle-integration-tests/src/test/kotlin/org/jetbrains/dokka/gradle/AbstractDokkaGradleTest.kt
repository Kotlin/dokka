package org.jetbrains.dokka.gradle


import com.intellij.rt.execution.junit.FileComparisonFailure
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


val testDataFolder: Path = Paths.get("testData")

val pluginClasspathData: Path = Paths.get("build", "createClasspathManifest", "dokka-plugin-classpath.txt")

val dokkaFatJarPathData: Path = pluginClasspathData.resolveSibling("fatjar.txt")

val androidLocalProperties = testDataFolder.resolve("android.local.properties").let { if (Files.exists(it)) it else null }

abstract class AbstractDokkaGradleTest {
    @get:Rule val testProjectDir = TemporaryFolder()

    open val pluginClasspath: List<File> = pluginClasspathData.toFile().readLines().map { File(it) }

    fun checkOutputStructure(expected: String, actualSubpath: String) {
        val expectedPath = testDataFolder.resolve(expected)
        val actualPath = testProjectDir.root.toPath().resolve(actualSubpath).normalize()

        assertEqualsIgnoringSeparators(expectedPath.toFile(), buildString {
            actualPath.toFile().writeStructure(this, File(actualPath.toFile(), "."))
        })
    }

    fun checkNoErrorClasses(actualSubpath: String, extension: String = "html", errorClassMarker: String = "ERROR CLASS") {
        val actualPath = testProjectDir.root.toPath().resolve(actualSubpath).normalize()
        var checked = 0
        Files.walk(actualPath).filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".$extension") }.forEach {
            val text = it.toFile().readText()

            val noErrorClasses = text.replace(errorClassMarker, "?!")

            if (noErrorClasses != text) {
                throw FileComparisonFailure("", noErrorClasses, text, null)
            }

            checked++
        }
        println("$checked files checked for error classes")
    }

    fun checkNoUnresolvedLinks(actualSubpath: String, extension: String = "html", marker: Regex = "[\"']#[\"']".toRegex()) {
        val actualPath = testProjectDir.root.toPath().resolve(actualSubpath).normalize()
        var checked = 0
        Files.walk(actualPath).filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".$extension") }.forEach {
            val text = it.toFile().readText()

            val noErrorClasses = text.replace(marker, "?!")

            if (noErrorClasses != text) {
                throw FileComparisonFailure("", noErrorClasses, text, null)
            }

            checked++
        }
        println("$checked files checked for unresolved links")
    }

    fun checkExternalLink(actualSubpath: String, linkBody: String, fullLink: String, extension: String = "html") {
        val match = "!!match!!"
        val notMatch = "!!not-match!!"

        val actualPath = testProjectDir.root.toPath().resolve(actualSubpath).normalize()
        var checked = 0
        var totalEntries = 0
        Files.walk(actualPath).filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".$extension") }.forEach {
            val text = it.toFile().readText()

            val textWithoutMatches = text.replace(fullLink, match)

            val textWithoutNonMatches = textWithoutMatches.replace(linkBody, notMatch)

            if (textWithoutNonMatches != textWithoutMatches) {

                val expected = textWithoutNonMatches.replace(notMatch, fullLink).replace(match, fullLink)
                val actual = textWithoutMatches.replace(match, fullLink)

                throw FileComparisonFailure("", expected, actual, null)
            }
            if (text != textWithoutMatches)
                totalEntries++

            checked++
        }
        println("$checked files checked for valid external links '$linkBody', found $totalEntries links")
    }

    fun configure(gradleVersion: String = "3.5", kotlinVersion: String = "1.1.2", arguments: Array<String>): GradleRunner {
        val fatjar = dokkaFatJarPathData.toFile().readText()

        return GradleRunner.create().withProjectDir(testProjectDir.root)
                .withArguments("-Pdokka_fatjar=$fatjar", "-Ptest_kotlin_version=$kotlinVersion", *arguments)
                .withPluginClasspath(pluginClasspath)
                .withGradleVersion(gradleVersion)
                .withDebug(true)
    }
}