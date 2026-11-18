/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reproduces https://github.com/Kotlin/dokka/issues/4620
 *
 * `Base` comes from a library dependency (`base-lib-4620.jar`) and has a private member whose type
 * (`repro.hidden.Logger`) comes from that library's `implementation` dependency, which is not
 * exposed to Dokka and is therefore missing from the analysis classpath
 *
 * The jar is built by compiling the sources below and excluding the `repro.hidden` classes from it:
 * ```kotlin
 * // repro/hidden/Logger.kt
 * package repro.hidden
 * class Logger
 *
 * // repro/lib/Base.kt
 * package repro.lib
 * abstract class Base {
 *     private val logger: repro.hidden.Logger = repro.hidden.Logger()
 * }
 * ```
 */
class UnresolvedPrivateMemberTypeTest : BaseAbstractTest() {
    private fun getResourceAbsolutePath(resourcePath: String): String {
        val resource = object {}.javaClass.classLoader.getResource(resourcePath)?.file
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        return File(resource).absolutePath
    }

    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
                // only `repro.lib` is on the classpath, `repro.hidden` is missing,
                // mimicking an `implementation` dependency not visible to Dokka
                classpath = listOf(getResourceAbsolutePath("jars/base-lib-4620.jar"))
            }
        }
    }

    @Test
    fun `should not warn about unresolved type of private member inherited from a library class`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |import repro.lib.Base
            |
            |class Derived : Base()
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = {
                assertTrue(
                    logger.warnMessages.isEmpty(),
                    "Private members must not be reported, " +
                            "but got warnings: ${logger.warnMessages.joinToString()}"
                )
            }
        }
    }

    @Test
    fun `should warn about unresolved type of private documented member inherited from a library class`() {
        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |import repro.lib.Base
            |
            |class Derived : Base()
            """.trimIndent(),
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/main/kotlin")
                        // only `repro.lib` is on the classpath, `repro.hidden` is missing,
                        // mimicking an `implementation` dependency not visible to Dokka
                        classpath = listOf(getResourceAbsolutePath("jars/base-lib-4620.jar"))
                        documentedVisibilities =
                            setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PRIVATE)
                    }
                }
            }
        ) {
            documentablesMergingStage = {
                val expectedWarnings = setOf(
                    "`repro.hidden.Logger` is unresolved in repro.lib/Base//#/PointingToDeclaration/",
                    "`repro.hidden.Logger` is unresolved in repro.lib/Base/logger/#/PointingToDeclaration/",
                )
                assertEquals(
                    expectedWarnings,
                    logger.warnMessages.toSet(),
                )
            }
        }
    }
}
