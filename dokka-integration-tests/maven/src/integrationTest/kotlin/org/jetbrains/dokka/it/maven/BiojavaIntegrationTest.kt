/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.maven

import org.jetbrains.dokka.it.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiojavaIntegrationTest : AbstractIntegrationTest(), TestOutputCopier {

    private val currentDokkaVersion: String = checkNotNull(System.getenv("DOKKA_VERSION"))
    private val mavenBinaryFile: File = File(checkNotNull(System.getenv("MVN_BINARY_PATH")))
    override val projectOutputLocation: File by lazy { File(projectDir, "biojava-core/target/dokkaJavadoc") }

    private val localSettingsXml: File by lazy {
        projectDir.resolve("local-settings.xml").apply {
            writeText(createSettingsXml())
        }
    }

    @BeforeTest
    fun prepareProjectFiles() {
        val bioJavaDir = File("projects", "biojava")
        val templateProjectDir = bioJavaDir.resolve("biojava")
        templateProjectDir.copyRecursively(projectDir)
        val customResourcesDir = File(templateProjectDir, "custom Resources")
        if (customResourcesDir.exists() && customResourcesDir.isDirectory) {
            customResourcesDir.copyRecursively(File(projectDir, "customResources"), overwrite = true)
        }
        copyAndApplyGitDiff(projectDir.toPath(), bioJavaDir.resolve("biojava.diff").toPath())
        projectDir.resolve("local-settings.xml").writeText(createSettingsXml())
    }

    @Test
    fun `dokka javadoc`() {
        val result = ProcessBuilder().directory(projectDir)
            .command(
                mavenBinaryFile.absolutePath,
                "dokka:javadoc",
                "--settings", localSettingsXml.invariantSeparatorsPath,
                "-pl",
                "biojava-core",
                "\"-Ddokka_version=$currentDokkaVersion\"",
                "-U",
                "-e"
            ).start().awaitProcessResult()

        diagnosticAsserts(result)

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        val scriptsDir = File(projectOutputLocation, "jquery")
        assertTrue(scriptsDir.isDirectory, "Missing jquery directory")

        val stylesDir = File(projectOutputLocation, "resources")
        assertTrue(stylesDir.isDirectory, "Missing resources directory")

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
    }
}
