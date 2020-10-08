package org.jetbrains.dokka.javadoc.validity

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiplatformConfiguredCheckerTest : AbstractCoreTest() {

    val mppConfig: DokkaConfigurationImpl = dokkaConfiguration {
        format = "javadoc"
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
                analysisPlatform = "jvm"
                externalDocumentationLinks = listOf(
                    ExternalDocumentationLink("https://docs.oracle.com/javase/8/docs/api/"),
                    ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
                )
            }
            sourceSet {
                sourceRoots = listOf("src")
                analysisPlatform = "js"
                externalDocumentationLinks = listOf(
                    ExternalDocumentationLink("https://docs.oracle.com/javase/8/docs/api/"),
                    ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
                )
            }
        }
    }

    val sppConfig: DokkaConfigurationImpl = dokkaConfiguration {
        format = "javadoc"
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
                analysisPlatform = "jvm"
                externalDocumentationLinks = listOf(
                    ExternalDocumentationLink("https://docs.oracle.com/javase/8/docs/api/"),
                    ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
                )
            }
        }
    }

    @Test
    fun `mpp config should fail for javadoc`() {
        testInline("", mppConfig) {
            verificationStage = { verification ->
                var mppDetected = false
                try {
                    verification()
                } catch (e: DokkaException) {
                    mppDetected =
                        e.localizedMessage == "Pre-generation validity check failed: ${MultiplatformConfiguredChecker.errorMessage}"
                }
                assertTrue(mppDetected, "MPP configuration not detected")
            }
        }
    }

    @Test
    fun `spp config should not fail for javadoc`() {
        testInline("", sppConfig) {
            verificationStage = { verification ->
                var mppDetected = false
                try {
                    verification()
                } catch (e: DokkaException) {
                    mppDetected =
                        e.localizedMessage == "Pre-generation validity check failed: ${MultiplatformConfiguredChecker.errorMessage}"
                }
                assertFalse(mppDetected, "SPP taken as multiplatform")
            }
        }
    }
}