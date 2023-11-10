/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
import utils.ParamAttributes
import utils.bareSignature
import utils.pWrapped
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class KotlinDeprecatedTest : BaseAbstractTest() {

    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `should assert util functions for deprecation`() {
        testInline(
            """
            |/src/main/kotlin/kotlin/KotlinFile.kt
            |package deprecated
            |
            |@Deprecated(
            |    message = "Fancy message"
            |)
            |fun simpleFunction() {} 
            """.trimIndent(),
            testConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val deprecatedFunction = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "simpleFunction" }

                val isDeprecated = (deprecatedFunction as WithExtraProperties<out Documentable>).isDeprecated()
                assertTrue(isDeprecated)

                val deprecatedAnnotation = (deprecatedFunction as WithExtraProperties<out Documentable>).deprecatedAnnotation
                assertNotNull(deprecatedAnnotation)

                assertTrue(deprecatedAnnotation.isDeprecated())
                assertEquals("kotlin", deprecatedAnnotation.dri.packageName)
                assertEquals("Deprecated", deprecatedAnnotation.dri.classNames)
            }
        }
    }

    @Test
    fun `should change header if deprecation level is not default`() {
        testInline(
            """
            |/src/main/kotlin/kotlin/DeprecatedKotlin.kt
            |package deprecated
            |
            |/**
            | * Average function description
            | */
            |@Deprecated(
            |    message = "Reason for deprecation bla bla",
            |    level = DeprecationLevel.ERROR
            |)
            |fun oldLegacyFunction(typedParam: SomeOldType, someLiteral: String): String {}
            |
            |fun newShinyFunction(typedParam: SomeOldType, someLiteral: String, newTypedParam: SomeNewType): String {}
            |class SomeOldType {}
            |class SomeNewType {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionWithDeprecatedFunction = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "oldLegacyFunction" } as ContentPage

                functionWithDeprecatedFunction.content.assertNode {
                    group {
                        header(1) { +"oldLegacyFunction" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "oldLegacyFunction",
                                    returnType = "String",
                                    params = arrayOf(
                                        "typedParam" to ParamAttributes(emptyMap(), emptySet(), "SomeOldType"),
                                        "someLiteral" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    )
                                )
                            }
                            after {
                                group {
                                    header(3) {
                                        +"Deprecated (with error)"
                                    }
                                    p {
                                        +"Reason for deprecation bla bla"
                                    }
                                }
                                group { pWrapped("Average function description") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should display repalceWith param with imports as code blocks`() {
        testInline(
            """
            |/src/main/kotlin/kotlin/DeprecatedKotlin.kt
            |package deprecated
            |
            |/**
            | * Average function description
            | */
            |@Deprecated(
            |    message = "Reason for deprecation bla bla",
            |    replaceWith = ReplaceWith(
            |        "newShinyFunction(typedParam, someLiteral, SomeNewType())",
            |        imports = [
            |            "com.example.dokka.debug.newShinyFunction",
            |            "com.example.dokka.debug.SomeOldType",
            |            "com.example.dokka.debug.SomeNewType",
            |        ]
            |    ),
            |)
            |fun oldLegacyFunction(typedParam: SomeOldType, someLiteral: String): String {}
            |
            |fun newShinyFunction(typedParam: SomeOldType, someLiteral: String, newTypedParam: SomeNewType): String {}
            |class SomeOldType {}
            |class SomeNewType {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionWithDeprecatedFunction = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "oldLegacyFunction" } as ContentPage

                functionWithDeprecatedFunction.content.assertNode {
                    group {
                        header(1) { +"oldLegacyFunction" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "oldLegacyFunction",
                                    returnType = "String",
                                    params = arrayOf(
                                        "typedParam" to ParamAttributes(emptyMap(), emptySet(), "SomeOldType"),
                                        "someLiteral" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    )
                                )
                            }
                            after {
                                group {
                                    header(3) {
                                        +"Deprecated"
                                    }
                                    p {
                                        +"Reason for deprecation bla bla"
                                    }

                                    header(4) {
                                        +"Replace with"
                                    }
                                    codeBlock {
                                        +"import com.example.dokka.debug.newShinyFunction"
                                        br()
                                        +"import com.example.dokka.debug.SomeOldType"
                                        br()
                                        +"import com.example.dokka.debug.SomeNewType"
                                        br()
                                    }
                                    codeBlock {
                                        +"newShinyFunction(typedParam, someLiteral, SomeNewType())"
                                    }
                                }
                                group { pWrapped("Average function description") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should add footnote for DeprecatedSinceKotlin annotation`() {
        testInline(
            """
            |/src/main/kotlin/kotlin/DeprecatedKotlin.kt
            |package deprecated
            |
            |/**
            | * Average function description
            | */
            |@DeprecatedSinceKotlin(
            |    warningSince = "1.4",
            |    errorSince = "1.5",
            |    hiddenSince = "1.6"
            |)
            |@Deprecated(
            |    message = "Deprecation reason bla bla"
            |)
            |fun oldLegacyFunction(typedParam: SomeOldType, someLiteral: String): String {}
            |
            |fun newShinyFunction(typedParam: SomeOldType, someLiteral: String, newTypedParam: SomeNewType): String {}
            |class SomeOldType {}
            |class SomeNewType {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionWithDeprecatedFunction = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "oldLegacyFunction" } as ContentPage

                functionWithDeprecatedFunction.content.assertNode {
                    group {
                        header(1) { +"oldLegacyFunction" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "oldLegacyFunction",
                                    returnType = "String",
                                    params = arrayOf(
                                        "typedParam" to ParamAttributes(emptyMap(), emptySet(), "SomeOldType"),
                                        "someLiteral" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    )
                                )
                            }
                            after {
                                group {
                                    header(3) {
                                        +"Deprecated"
                                    }
                                    group {
                                        check { assertEquals(ContentStyle.Footnote, this.style.firstOrNull()) }
                                        p {
                                            +"Warning since 1.4"
                                        }
                                        p {
                                            +"Error since 1.5"
                                        }
                                        p {
                                            +"Hidden since 1.6"
                                        }
                                    }
                                    p {
                                        +"Deprecation reason bla bla"
                                    }
                                }
                                group { pWrapped("Average function description") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should generate deprecation block with all parameters present and long description`() {
        testInline(
            """
            |/src/main/kotlin/kotlin/DeprecatedKotlin.kt
            |package deprecated
            |
            |/**
            | * Average function description
            | */
            |@Deprecated(
            |    message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas vel vulputate risus. " +
            |            "Etiam dictum odio vel vulputate auctor.Nulla facilisi. Duis ullamcorper ullamcorper lectus " +
            |            "nec rutrum. Quisque eu risus eu purus bibendum ultricies. Maecenas tincidunt dui in sodales " +
            |            "faucibus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin id sem felis. " +
            |            "Praesent et libero lacinia, egestas libero in, ultrices lectus. Suspendisse eget volutpat " +
            |            "velit. Phasellus laoreet mi eu egestas mattis.",
            |    replaceWith = ReplaceWith(
            |        "newShinyFunction(typedParam, someLiteral, SomeNewType())",
            |        imports = [
            |            "com.example.dokka.debug.newShinyFunction",
            |            "com.example.dokka.debug.SomeOldType",
            |            "com.example.dokka.debug.SomeNewType",
            |        ]
            |    ),
            |    level = DeprecationLevel.ERROR
            |)
            |fun oldLegacyFunction(typedParam: SomeOldType, someLiteral: String): String {}
            |
            |fun newShinyFunction(typedParam: SomeOldType, someLiteral: String, newTypedParam: SomeNewType): String {}
            |class SomeOldType {}
            |class SomeNewType {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionWithDeprecatedFunction = module.children
                    .single { it.name == "deprecated" }.children
                    .single { it.name == "oldLegacyFunction" } as ContentPage

                functionWithDeprecatedFunction.content.assertNode {
                    group {
                        header(1) { +"oldLegacyFunction" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "oldLegacyFunction",
                                    returnType = "String",
                                    params = arrayOf(
                                        "typedParam" to ParamAttributes(emptyMap(), emptySet(), "SomeOldType"),
                                        "someLiteral" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    )
                                )
                            }
                            after {
                                group {
                                    header(3) {
                                        +"Deprecated (with error)"
                                    }
                                    p {
                                        +("Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                                                "Maecenas vel vulputate risus. Etiam dictum odio vel " +
                                                "vulputate auctor.Nulla facilisi. Duis ullamcorper " +
                                                "ullamcorper lectus nec rutrum. Quisque eu risus eu " +
                                                "purus bibendum ultricies. Maecenas tincidunt dui in sodales faucibus. " +
                                                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                                                "Proin id sem felis. Praesent et libero lacinia, egestas " +
                                                "libero in, ultrices lectus. Suspendisse eget volutpat velit. " +
                                                "Phasellus laoreet mi eu egestas mattis.")
                                    }
                                    header(4) {
                                        +"Replace with"
                                    }
                                    codeBlock {
                                        +"import com.example.dokka.debug.newShinyFunction"
                                        br()
                                        +"import com.example.dokka.debug.SomeOldType"
                                        br()
                                        +"import com.example.dokka.debug.SomeNewType"
                                        br()
                                    }
                                    codeBlock {
                                        +"newShinyFunction(typedParam, someLiteral, SomeNewType())"
                                    }
                                }
                                group { pWrapped("Average function description") }
                            }
                        }
                    }
                }
            }
        }
    }
}
