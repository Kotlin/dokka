package org.jetbrains.dokka.gradle


import com.intellij.rt.execution.junit.FileComparisonFailure
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


val testDataFolder = Paths.get("testData")

val pluginClasspathData = Paths.get("build", "createClasspathManifest", "plugin-classpath.txt")

val dokkaFatJarPathData = pluginClasspathData.resolveSibling("fatjar.txt")

abstract class AbstractBasicDokkaGradleTest {
    @get:Rule val testProjectDir = TemporaryFolder()

    fun checkExpectedOutput(root: String, actualSubpath: String) {
        val rootPath = testDataFolder.resolve(root)
        val tmpRoot = testProjectDir.root.toPath().resolve(actualSubpath).normalize()

        val actualFiles = Files.walk(tmpRoot).filter { Files.isRegularFile(it) }.collect(Collectors.toList()) as List<Path>
        val expectedFiles = Files.walk(rootPath).filter { Files.isRegularFile(it) }.collect(Collectors.toList()) as List<Path>

        val matchedActualFiles = mutableListOf<Path>()

        expectedFiles
                .map { expected: Path -> Pair(expected, actualFiles.find { it.endsWith(expected.relativize(rootPath)) }) }
                .forEach { p ->
                    val expected = p.first
                    val actual = p.second
                    assertEqualsIgnoringSeparators(expected.toFile(), actual?.toFile())
                    matchedActualFiles.add(actual!!)
                }


        actualFiles.filter { it !in matchedActualFiles }.forEach { actual ->
            val expected = rootPath.resolve(actual.relativize(tmpRoot))
            assertEqualsIgnoringSeparators(expected.toFile(), actual.toFile())
        }
    }

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


    fun configure(gradleVersion: String = "3.5", kotlinVersion: String = "1.1.2", arguments: Array<String>): GradleRunner {
        val fatjar = dokkaFatJarPathData.toFile().readText()
        val pluginClasspath = pluginClasspathData.toFile().readLines().map { File(it) }

        return GradleRunner.create().withProjectDir(testProjectDir.root)
                .withArguments("-Pdokka_fatjar=$fatjar", "-Ptest_kotlin_version=$kotlinVersion", *arguments)
                .withPluginClasspath(pluginClasspath)
                .withGradleVersion(gradleVersion)
                .withDebug(true)
    }


}