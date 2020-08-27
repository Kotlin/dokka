package org.jetbrains.dokka.it.cli

import org.jetbrains.dokka.it.awaitProcessResult
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliIntegrationTest : AbstractCliIntegrationTest() {

    @BeforeTest
    fun copyProject() {
        val templateProjectDir = File("projects", "it-cli")
        templateProjectDir.copyRecursively(projectDir)
    }

    @Test
    fun runHelp() {
        val process = ProcessBuilder("java", "-jar", cliJarFile.path, "-h")
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")
        assertTrue("Usage: " in result.output)
    }

    @Test
    fun runCli() {
        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.mkdirs())
        val process = ProcessBuilder(
            "java", "-jar", cliJarFile.path,
            "-outputDir", dokkaOutputDir.path,
            "-pluginsClasspath", basePluginJarFile.path,
            "-moduleName", "Basic Project",
            "-sourceSet",
            buildString {
                append(" -sourceSetName cliMain")
                append(" -src ${File(projectDir, "src").path}")
                append(" -jdkVersion 8")
                append(" -analysisPlatform jvm")
                append(" -reportUndocumented")
                append(" -skipDeprecated")
            }
        )
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")

        val extensionLoadedRegex = Regex("""Extension: org\.jetbrains\.dokka\.base\.DokkaBase""")
        val amountOfExtensionsLoaded = extensionLoadedRegex.findAll(result.output).count()

        assertTrue(
            amountOfExtensionsLoaded > 10,
            "Expected more than 10 extensions being present (found $amountOfExtensionsLoaded)"
        )

        val undocumentedReportRegex = Regex("""Undocumented:""")
        val amountOfUndocumentedReports = undocumentedReportRegex.findAll(result.output).count()
        assertTrue(
            amountOfUndocumentedReports > 0,
            "Expected at least one report of undocumented code (found $amountOfUndocumentedReports)"
        )

        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        val imagesDir = File(dokkaOutputDir, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(dokkaOutputDir, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")

        val stylesDir = File(dokkaOutputDir, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")

        val navigationHtml = File(dokkaOutputDir, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")

        projectDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
