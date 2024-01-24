/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.annotations

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.documentables.deprecatedAnnotation
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.ContentStyle
import utils.pWrapped
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JavaDeprecatedTest : BaseAbstractTest() {

    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should assert util functions for deprecation`() {
        testInline(
            """
            |/src/main/kotlin/deprecated/DeprecatedJavaClass.java
            |package deprecated
            |
            |@Deprecated(forRemoval = true)
            |public class DeprecatedJavaClass {}
            """.trimIndent(),
            testConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val deprecatedClass = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "DeprecatedJavaClass" }

                val isDeprecated = (deprecatedClass as WithExtraProperties<out Documentable>).isDeprecated()
                assertTrue(isDeprecated)

                val deprecatedAnnotation = (deprecatedClass as WithExtraProperties<out Documentable>).deprecatedAnnotation
                assertNotNull(deprecatedAnnotation)

                assertTrue(deprecatedAnnotation.isDeprecated())
                assertEquals("java.lang", deprecatedAnnotation.dri.packageName)
                assertEquals("Deprecated", deprecatedAnnotation.dri.classNames)
            }
        }
    }

    @Test
    fun `should change deprecated header if marked for removal`() {
        testInline(
            """
            |/src/main/kotlin/deprecated/DeprecatedJavaClass.java
            |package deprecated
            |
            |/**
            | * Average function description
            | */
            |@Deprecated(forRemoval = true)
            |public class DeprecatedJavaClass {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val deprecatedJavaClass = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "DeprecatedJavaClass" } as ContentPage

                deprecatedJavaClass.content.assertNode {
                    group {
                        header(1) { +"DeprecatedJavaClass" }
                        platformHinted {
                            skipAllNotMatching()
                            group {
                                header(3) {
                                    +"Deprecated (for removal)"
                                }
                            }
                            group { pWrapped("Average function description") }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `should add footnote for 'since' param`() {
        testInline(
            """
            |/src/main/kotlin/deprecated/DeprecatedJavaClass.java
            |package deprecated
            |
            |/**
            | * Average function description
            | */
            |@Deprecated(since = "11")
            |public class DeprecatedJavaClass {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val deprecatedJavaClass = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "DeprecatedJavaClass" } as ContentPage

                deprecatedJavaClass.content.assertNode {
                    group {
                        header(1) { +"DeprecatedJavaClass" }
                        platformHinted {
                            skipAllNotMatching()
                            group {
                                header(3) {
                                    +"Deprecated"
                                }
                                group {
                                    check { assertEquals(ContentStyle.Footnote, this.style.firstOrNull()) }
                                    +"Since version 11"
                                }
                            }
                            group { pWrapped("Average function description") }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }
}
