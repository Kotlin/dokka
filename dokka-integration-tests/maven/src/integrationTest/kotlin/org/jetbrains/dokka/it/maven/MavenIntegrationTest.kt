/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.maven

import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.ProcessResult
import org.jetbrains.dokka.it.awaitProcessResult
import java.io.File
import kotlin.test.*

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
        val customResourcesDir = File(templateProjectDir, "customResources")
        if (customResourcesDir.exists() && customResourcesDir.isDirectory) {
            customResourcesDir.copyRecursively(File(projectDir, "customResources"), overwrite = true)
        }
    }

    @Test
    fun `dokka help`() {
        val result = ProcessBuilder().directory(projectDir)
            .command(mavenBinaryFile.absolutePath, "dokka:help", "-U", "-e")
            .start()
            .awaitProcessResult()

        // format the output to remove blank lines and make newlines system-independent
        val output = result.output.lines().filter { it.isNotBlank() }.joinToString("\n")

        assertContains(
            output,
            """
                |This plugin has 4 goals:
                |dokka:dokka
                |dokka:help
                |dokka:javadoc
                |dokka:javadocJar
            """.trimMargin()
        )
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

        assertTrue(
            stylesDir.resolve("logo-styles.css").readText().contains(
                "--dokka-logo-image-url: url('https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg');",
            )
        )
        assertTrue(stylesDir.resolve("custom-style-to-add.css").isFile)
        projectDir.allHtmlFiles().forEach { file ->
            if (file.name != "navigation.html") {
                assertTrue(
                    "custom-style-to-add.css" in file.readText(),
                    "custom styles not added to html file ${file.name}"
                )
            }
        }
        assertTrue(stylesDir.resolve("custom-style-to-add.css").readText().contains("""/* custom stylesheet */"""))
        assertTrue(imagesDir.resolve("custom-resource.svg").isFile)

        assertConfiguredVisibility(projectDir)
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

    private fun assertConfiguredVisibility(projectDir: File) {
        val projectHtmlFiles = projectDir.allHtmlFiles().toList()

        assertContentVisibility(
            contentFiles = projectHtmlFiles,
            documentPublic = true,
            documentProtected = true, // sourceSet documentedVisibilities
            documentInternal = false,
            documentPrivate = true // for overriddenVisibility package
        )

        assertContainsFilePaths(
            outputFiles = projectHtmlFiles,
            expectedFilePaths = listOf(
                // documentedVisibilities is overridden for package `overriddenVisibility` specifically
                // to include private code, so html pages for it are expected to have been created
                Regex("it\\.overriddenVisibility/-visible-private-class/private-method\\.html"),
                Regex("it\\.overriddenVisibility/-visible-private-class/private-val\\.html"),
            )
        )
    }

    companion object {
        /*
         * TODO replace with kotlin.test.assertContains after migrating to Kotlin language version 1.5+
         */
        fun assertContains(
            charSequence: CharSequence,
            @Language("TEXT") other: CharSequence,
            ignoreCase: Boolean = false
        ) {
            asserter.assertTrue(
                { "Expected the char sequence to contain the substring.\nCharSequence <$charSequence>, substring <$other>, ignoreCase <$ignoreCase>." },
                charSequence.contains(other, ignoreCase)
            )
        }
    }
}
