/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.cli

import org.jetbrains.dokka.it.awaitProcessResult
import java.io.File
import java.io.PrintWriter
import java.lang.IllegalStateException
import kotlin.test.*

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

        assertContentVisibility(
            contentFiles = projectDir.allHtmlFiles().toList(),
            documentPublic = true,
            documentInternal = false,
            documentProtected = false,
            documentPrivate = false
        )

        assertFalse(
            projectDir.resolve("output").resolve("index.html").readText().contains("emptypackagetest"),
            "Expected not to render empty packages"
        )
    }

    @Test
    fun failCli() {
        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.mkdirs())
        val process = ProcessBuilder(
            "java", "-jar", cliJarFile.path,
            "-outputDir", dokkaOutputDir.path,
            "-pluginsClasspath", basePluginJarFile.path,
            "-moduleName", "Basic Project",
            "-failOnWarning",
            "-sourceSet",
            buildString {
                append(" -sourceSetName cliMain")
                append(" -src ${File(projectDir, "src").path}")
                append(" -jdkVersion 8")
                append(" -analysisPlatform jvm")
                append(" -reportUndocumented")
            }
        )
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(1, result.exitCode, "Expected exitCode 1 (Fail)")

        assertTrue(result.output.contains("Exception in thread \"main\" org.jetbrains.dokka.DokkaException: Failed with warningCount"))
    }

    @Test
    fun emptyPackagesTest() {
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
                append(" -noSkipEmptyPackages")
            }
        )
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")

        assertTrue(
            projectDir.resolve("output").resolve("index.html").readText().contains("emptypackagetest"),
            "Expected to render empty packages"
        )
    }

    @Test
    fun `module name should be optional`() {
        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.mkdirs())
        val process = ProcessBuilder(
            "java", "-jar", cliJarFile.path,
            "-outputDir", dokkaOutputDir.path,
            "-loggingLevel", "DEBUG",
            "-pluginsClasspath", basePluginJarFile.path,
            "-sourceSet",
            buildString {
                append(" -src ${File(projectDir, "src").path}")
            }
        )
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")
        assertTrue(result.output.contains("Loaded plugins: "), "Expected output to not contain info logs")

        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        val imagesDir = File(dokkaOutputDir, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(dokkaOutputDir, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")

        val stylesDir = File(dokkaOutputDir, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")

        val navigationHtml = File(dokkaOutputDir, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")
    }

    @Test
    fun `logging level should be respected`() {
        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.mkdirs())
        val process = ProcessBuilder(
            "java", "-jar", cliJarFile.path,
            "-outputDir", dokkaOutputDir.path,
            "-loggingLevel", "WARN",
            "-pluginsClasspath", basePluginJarFile.path,
            "-sourceSet",
            buildString {
                append(" -src ${File(projectDir, "src").path}")
            }
        )
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")
        assertFalse(result.output.contains("Loaded plugins: "), "Expected output to not contain info logs")
    }

    @Test
    fun `custom documented visibility`() {
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
                append(" -documentedVisibilities PUBLIC;PROTECTED")
                append(" -perPackageOptions it.overriddenVisibility.*,+visibility:PRIVATE")
            }
        )
            .redirectErrorStream(true)
            .start()

        val result = process.awaitProcessResult()
        assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")

        val allHtmlFiles = projectDir.allHtmlFiles().toList()

        assertContentVisibility(
            contentFiles = allHtmlFiles,
            documentPublic = true,
            documentProtected = true, // sourceSet documentedVisibilities
            documentInternal = false,
            documentPrivate = true // for overriddenVisibility package
        )

        assertContainsFilePaths(
            outputFiles = allHtmlFiles,
            expectedFilePaths = listOf(
                // documentedVisibilities is overridden for package `overriddenVisibility` specifically
                // to include private code, so html pages for it are expected to have been created
                Regex("it\\.overriddenVisibility/-visible-private-class/private-method\\.html"),
                Regex("it\\.overriddenVisibility/-visible-private-class/private-val\\.html"),
            )
        )
    }


    @Test
    fun `should accept json as input configuration`() {
        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.mkdirs())
        val resourcePath = javaClass.getResource("/my-file.json")?.toURI() ?: throw IllegalStateException("No JSON found!")
        val jsonPath = File(resourcePath).absolutePath
        PrintWriter(jsonPath).run {
            write(jsonBuilder(dokkaOutputDir.invariantSeparatorsPath, basePluginJarFile.invariantSeparatorsPath, File(projectDir, "src").invariantSeparatorsPath, reportUndocumented = true))
            close()
        }

        val process = ProcessBuilder(
            "java", "-jar", cliJarFile.path, jsonPath
        ).redirectErrorStream(true).start()

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
    }

    /**
     * This test disables global `reportUndocumneted` property and set `reportUndocumented` via perPackageOptions to
     * make sure that global settings apply to dokka context.
     */
    @Test
    fun `global settings should overwrite package options in configuration`() {
        val dokkaOutputDir = File(projectDir, "output")
        assertTrue(dokkaOutputDir.mkdirs())
        val resourcePath = javaClass.getResource("/my-file.json")?.toURI() ?: throw IllegalStateException("No JSON found!")
        val jsonPath = File(resourcePath).absolutePath
        PrintWriter(jsonPath).run {
            write(
                jsonBuilder(
                    outputPath = dokkaOutputDir.invariantSeparatorsPath,
                    pluginsClasspath = basePluginJarFile.invariantSeparatorsPath,
                    projectPath = File(projectDir, "src").invariantSeparatorsPath,
                    globalSourceLinks = """
                        {
                          "localDirectory": "/home/Vadim.Mishenev/dokka/examples/cli/src/main/kotlin",
                          "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example/src/main/kotlin",
                          "remoteLineSuffix": "#L"
                        }
                    """.trimIndent(),
                    globalExternalDocumentationLinks = """
                        {
                          "url": "https://docs.oracle.com/javase/8/docs/api/",
                          "packageListUrl": "https://docs.oracle.com/javase/8/docs/api/package-list"
                        },
                        {
                          "url": "https://kotlinlang.org/api/latest/jvm/stdlib/",
                          "packageListUrl": "https://kotlinlang.org/api/latest/jvm/stdlib/package-list"
                        }
                        """.trimIndent(),
                    globalPerPackageOptions = """
                        {
                          "matchingRegex": ".*",
                          "skipDeprecated": "true",
                          "reportUndocumented": "true", 
                          "documentedVisibilities": ["PUBLIC", "PRIVATE", "PROTECTED", "INTERNAL", "PACKAGE"]
                        }
                    """.trimIndent(),
                    reportUndocumented = false
                ),
            )
            close()
        }

        val process = ProcessBuilder(
            "java", "-jar", cliJarFile.path, jsonPath
        ).redirectErrorStream(true).start()

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
    }

    @Test
    fun `relative paths in configuraiton should work`() {
        val resourcePath =
            javaClass.getResource("/my-file.json")?.toURI() ?: throw IllegalStateException("No JSON found!")
        val jsonPath = File(resourcePath)

        val dokkaOutputDir = File(projectDir, "output-relative")
        assertTrue(dokkaOutputDir.mkdirs())
        jsonPath.writeText(
            jsonBuilder(
                outputPath = dokkaOutputDir.invariantSeparatorsPath,
                pluginsClasspath = basePluginJarFile.absoluteFile.invariantSeparatorsPath,
                projectPath = "src", // relative path
            )
        )

        ProcessBuilder(
            "java", "-jar", cliJarFile.absolutePath, jsonPath.absolutePath
        ).directory(projectDir).redirectErrorStream(true).start().also { process ->
            val result = process.awaitProcessResult()
            assertEquals(0, result.exitCode, "Expected exitCode 0 (Success)")
        }

        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        val htmlFiles = dokkaOutputDir.allHtmlFiles().map { it.relativeTo(dokkaOutputDir).path }.toList()

        // check that both Kotlin and Java sources are processed

        // kotlin:
        assertContains(htmlFiles, "-dokka -example/it.basic/index.html")
        assertContains(htmlFiles, "-dokka -example/it.basic/-public-class/public-documented-function.html")

        // java:
        assertContains(htmlFiles, "-dokka -example/it.basic.java/index.html")
        assertContains(htmlFiles, "-dokka -example/it.basic.java/-sample-java-class/public-documented-function.html")
    }
}
