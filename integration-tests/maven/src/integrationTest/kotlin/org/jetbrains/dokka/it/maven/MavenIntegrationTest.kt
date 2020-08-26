package org.jetbrains.dokka.it.maven

import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.awaitProcessResult
import org.jetbrains.dokka.it.ProcessResult
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MavenIntegrationTest : AbstractIntegrationTest() {

    private val currentDokkaVersion: String = checkNotNull(System.getenv("DOKKA_VERSION"))

    private val mavenBinaryFile: File = File(checkNotNull(System.getenv("MVN_BINARY_PATH")))

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-maven")
        templateProjectDir.copyRecursively(projectDir)
        val pomXml = File(projectDir, "pom.xml")
        assertTrue(pomXml.isFile)
        pomXml.apply {
            writeText(readText().replace("\$dokka_version", currentDokkaVersion))
        }
    }

    @Test
    fun `dokka dokka`() {
        val result = ProcessBuilder().directory(projectDir)
            .command(mavenBinaryFile.absolutePath, "dokka:dokka", "-U", "-e").start().awaitProcessResult()

        diagnosticAsserts(result)

        val dokkaOutputDir = File(projectDir, "output")
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

    @Test
    fun `dokka javadoc`() {
        val result = ProcessBuilder().directory(projectDir)
            .command(mavenBinaryFile.absolutePath, "dokka:javadoc", "-U", "-e").start().awaitProcessResult()

        diagnosticAsserts(result)

        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        val scriptsDir = File(dokkaOutputDir, "jquery")
        assertTrue(scriptsDir.isDirectory, "Missing jquery directory")

        val stylesDir = File(dokkaOutputDir, "resources")
        assertTrue(stylesDir.isDirectory, "Missing resources directory")

        projectDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
        }
    }

    @Test
    fun `dokka javadocJar`() {
        val result = ProcessBuilder().directory(projectDir)
            .command(mavenBinaryFile.absolutePath, "dokka:javadocJar", "-U", "-e").start().awaitProcessResult()

        diagnosticAsserts(result)

        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        val scriptsDir = File(dokkaOutputDir, "jquery")
        assertTrue(scriptsDir.isDirectory, "Missing jquery directory")

        val stylesDir = File(dokkaOutputDir, "resources")
        assertTrue(stylesDir.isDirectory, "Missing resources directory")

        val dokkaTargetDir = File(projectDir, "target")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka target directory")

        val jarFile = File(dokkaTargetDir, "it-maven-1.0-SNAPSHOT-javadoc.jar")
        assertTrue(jarFile.isFile, "Missing dokka jar file")

        projectDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
        }
    }

    private fun diagnosticAsserts(result: ProcessResult) {
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

        val undocumentedJavaReportRegex = Regex("""Undocumented: it\.basic\.java""")
        val amountOfUndocumentedJavaReports = undocumentedJavaReportRegex.findAll(result.output).count()
        assertTrue(
            amountOfUndocumentedJavaReports > 0,
            "Expected at least one report of undocumented java code (found $amountOfUndocumentedJavaReports)"
        )
    }
}
