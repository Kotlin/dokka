package org.jetbrains.dokka.it.maven

import org.jetbrains.dokka.it.*
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiojavaIntegrationTest : AbstractIntegrationTest(), S3Project {

    private val currentDokkaVersion: String = checkNotNull(System.getenv("DOKKA_VERSION"))
    private val mavenBinaryFile: File = File(checkNotNull(System.getenv("MVN_BINARY_PATH")))
    override val projectOutputLocation: File by lazy { File(projectDir, "biojava-core/target/dokkaJavadoc") }

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "biojava/biojava")
        templateProjectDir.copyRecursively(projectDir)
        val customResourcesDir = File(templateProjectDir, "custom Resources")
        if (customResourcesDir.exists() && customResourcesDir.isDirectory) {
            customResourcesDir.copyRecursively(File(projectDir, "customResources"), overwrite = true)
        }
        copyAndApplyGitDiff(File("projects", "biojava/biojava.diff"))
    }

    @Test
    fun `dokka javadoc`() {
        val result = ProcessBuilder().directory(projectDir)
            .command(mavenBinaryFile.absolutePath, "dokka:javadoc", "-pl", "biojava-core", "\"-Ddokka_version=$currentDokkaVersion\"", "-U", "-e").start().awaitProcessResult()

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
