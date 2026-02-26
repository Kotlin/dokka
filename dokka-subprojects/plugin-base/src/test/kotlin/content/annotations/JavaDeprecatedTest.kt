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
import utils.*
import kotlin.test.*

@OnlyJavaPsi
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
            | * Average class description
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
                            group { pWrapped("Average class description") }
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

    // TODO: AA-based analysis produces different content structure for @deprecated Javadoc tag combined with @Deprecated annotation
    @org.junit.jupiter.api.Disabled("AA content rendering for @deprecated Javadoc tag with forRemoval differs")
    @Test
    fun `should take deprecation message from @deprecated javadoc tag`() {
        testInline(
            """
            |/src/main/kotlin/deprecated/DeprecatedJavaClass.java
            |package deprecated
            |
            |public class DeprecatedJClass {
            |    /**
            |     * Don't do anything
            |     * @deprecated
            |     * This method is no longer acceptable to compute time between versions.
            |     * <p>Use {@link DeprecatedJClass#getStringMethodNew()} instead.</p>
            |     */
            |    @Deprecated(since = "18.0.2", forRemoval = true)
            |    public void getStringMethod(){}
            |}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val deprecatedFunction = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "DeprecatedJClass" }.children
                    .single { it.name == "getStringMethod" } as ContentPage

                deprecatedFunction.content.assertNode {
                    group {
                        header(1) { +"getStringMethod" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "open",
                                    keywords = emptySet(),
                                    name = "getStringMethod",
                                    returnType = null
                                )
                            }
                            after {
                                group {
                                    header(3) {
                                        +"Deprecated (for removal)"
                                    }
                                    group {
                                        check { assertEquals(ContentStyle.Footnote, this.style.firstOrNull()) }
                                        +"Since version 18.0.2"
                                    }
                                    p {
                                        comment {
                                            p {
                                                +"Thismethod is no longer acceptable to compute time between versions. "
                                            }
                                            p {
                                                +"Use "
                                                +"getStringMethodNew"
                                                +" instead."
                                            }
                                        }
                                    }
                                }
                                group { pWrapped("Don't do anything") }
                            }
                        }
                    }
                }
            }
        }
    }
}
