package org.jetbrains.dokka

import junit.framework.Assert.assertTrue
import org.junit.Test
import java.lang.IllegalStateException
import kotlin.test.assertEquals

class CliIntegrationTest {

    @Test
    fun testGlobalArgs() {
        val jsonPath = javaClass.getResource("/my-file.json")?.path ?: throw IllegalStateException("No JSON found!")
        val globalArguments = GlobalArguments(arrayOf(jsonPath))

        val configuration = initializeConfiguration(globalArguments)

        configuration.sourceSets.forEach {
            assertTrue(it.perPackageOptions.isNotEmpty())
            assertTrue(it.sourceLinks.isNotEmpty())
            assertTrue(it.externalDocumentationLinks.isNotEmpty())

            assertTrue(it.externalDocumentationLinks.any { it.url.toString() == "https://docs.oracle.com/javase/8/docs/api/" })
            assertEquals(it.sourceLinks.single().localDirectory, "/home/Vadim.Mishenev/dokka/examples/cli/src/main/kotlin")
            assertEquals(it.perPackageOptions.single().matchingRegex, "my-custom-regex")
        }

    }

}
