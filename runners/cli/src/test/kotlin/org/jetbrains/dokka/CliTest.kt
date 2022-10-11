package org.jetbrains.dokka

import org.junit.Test
import java.lang.IllegalStateException
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliIntegrationTest {

    @Test
    fun `should apply global settings to all source sets`() {
        val jsonPath =
            Paths.get(javaClass.getResource("/my-file.json")?.toURI() ?: throw IllegalStateException("No JSON found!"))
                .toFile().toString()
        val globalArguments = GlobalArguments(arrayOf(jsonPath))

        val configuration = initializeConfiguration(globalArguments)

        configuration.sourceSets.forEach {
            assertTrue(it.perPackageOptions.isNotEmpty())
            assertTrue(it.sourceLinks.isNotEmpty())
            assertTrue(it.externalDocumentationLinks.isNotEmpty())

            assertTrue(it.externalDocumentationLinks.any { it.url.toString() == "https://docs.oracle.com/javase/8/docs/api/" })
            assertEquals(
                it.sourceLinks.single().localDirectory,
                "/home/Vadim.Mishenev/dokka/examples/cli/src/main/kotlin"
            )
            assertEquals(it.perPackageOptions.single().matchingRegex, "my-custom-regex")
        }

    }

    @Test
    fun `should not fail when no sourceset options are specified`() {
        val jsonPath = Paths.get(
            javaClass.getResource("/my-file-no-sourceset-options.json")?.toURI()
                ?: throw IllegalStateException("No JSON found!")
        ).toFile().toString()
        val globalArguments = GlobalArguments(arrayOf(jsonPath))

        val configuration = initializeConfiguration(globalArguments)

        configuration.sourceSets.forEach {
            assertTrue(it.perPackageOptions.isEmpty())
            assertTrue(it.sourceLinks.isEmpty())
            assertTrue(it.externalDocumentationLinks.size == 2) // there are default values, java and kotlin stdlibs
        }
    }

    @Test
    fun `should parse extra options`() {
        val globalArguments = GlobalArguments(
            arrayOf(
                "-moduleName", "module",
                "-extraOptions", "-extraOptions2=sdds",
            )
        )
        assertEquals("module", globalArguments.moduleName)
        assertEquals(listOf("-extraOptions", "-extraOptions2=sdds"), globalArguments.extraOptions)
    }
}
