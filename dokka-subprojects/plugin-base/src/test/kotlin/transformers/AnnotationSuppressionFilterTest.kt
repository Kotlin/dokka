/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AnnotationSuppressionFilterTest : BaseAbstractTest() {

    @Test
    fun `should suppress class by annotation`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedAnnotations = setOf("test.SuppressMe")
                }
            }
        }

        testInline(
            """
            /src/test/Annotated.kt
            package test
            annotation class SuppressMe

            @SuppressMe
            class Annotated

            class NotAnnotated
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val classNames = pkg.classlikes.map { it.name }
                // SuppressMe itself should be present, but Annotated should be suppressed
                assertEquals(setOf("SuppressMe", "NotAnnotated"), classNames.toSet())
                assertNull(pkg.classlikes.find { it.name == "Annotated" })
            }
        }
    }

    @Test
    fun `should suppress function by annotation`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedAnnotations = setOf("test.SuppressMe")
                }
            }
        }

        testInline(
            """
            /src/test/Functions.kt
            package test
            annotation class SuppressMe

            class Container {
                @SuppressMe
                fun annotated() {}

                fun notAnnotated() {}
            }
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val container = pkg.classlikes.single { it.name == "Container" } as DClass
                val functionNames = container.functions.map { it.name }
                assertEquals(listOf("notAnnotated"), functionNames)
            }
        }
    }

    @Test
    fun `should suppress by multiple annotations`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedAnnotations = setOf("test.SuppressMe1", "test.SuppressMe2")
                }
            }
        }

        testInline(
            """
            /src/test/Multiple.kt
            package test
            annotation class SuppressMe1
            annotation class SuppressMe2

            @SuppressMe1
            class Suppressed1

            @SuppressMe2
            class Suppressed2

            class NotSuppressed
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                val classNames = pkg.classlikes.map { it.name }
                assertEquals(setOf("SuppressMe1", "SuppressMe2", "NotSuppressed"), classNames.toSet())
            }
        }
    }

    @Test
    fun `should handle empty suppressedAnnotations`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    suppressedAnnotations = emptySet<String>()
                }
            }
        }

        testInline(
            """
            /src/test/Empty.kt
            package test
            annotation class SuppressMe

            @SuppressMe
            class Annotated
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single { it.name == "test" }
                assertNotNull(pkg.classlikes.find { it.name == "Annotated" })
            }
        }
    }
}
