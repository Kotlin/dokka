/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.annotations

import matchers.content.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinVersion
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.ContentPage
import signatures.AbstractRenderingTest
import utils.*
import kotlin.test.*


class SinceKotlinTest : AbstractRenderingTest() {

    val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
                analysisPlatform = "jvm"
            }
        }
    }

    @BeforeTest
    fun setSystemProperty() {
        System.setProperty(SinceKotlinTransformer.SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP, "true")
    }
    @AfterTest
    fun clearSystemProperty() {
        System.clearProperty(SinceKotlinTransformer.SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP)
    }

    @Test
    fun versionsComparing() {
        assertTrue(SinceKotlinVersion("1.0").compareTo(SinceKotlinVersion("1.0")) == 0)
        assertTrue(SinceKotlinVersion("1.0.0").compareTo(SinceKotlinVersion("1")) == 0)
        assertTrue(SinceKotlinVersion("1.0") >= SinceKotlinVersion("1.0"))
        assertTrue(SinceKotlinVersion("1.1") > SinceKotlinVersion("1"))
        assertTrue(SinceKotlinVersion("1.0") < SinceKotlinVersion("2.0"))
        assertTrue(SinceKotlinVersion("1.0") < SinceKotlinVersion("2.2"))
    }

    @Test
    fun `rendered SinceKotlin custom tag for typealias, extensions, functions, properties`() {
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@SinceKotlin("1.5")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
            |@SinceKotlin("1.5")
            |fun String.extension(abc: String): String {
            |    return "My precious " + abc
            |}            
            |@SinceKotlin("1.5")
            |typealias Str = String
            |@SinceKotlin("1.5")
            |val str = "str"
        """.trimIndent(),
            testConfiguration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.renderedContent("root/test/index.html")
                assertEquals(4, content.getElementsContainingOwnText("Since Kotlin").count())
            }
        }
    }

    @Test
    fun `should propagate SinceKotlin`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@SinceKotlin("1.5")
            |class A {
            |   fun ring(abc: String): String {
            |       return "My precious " + abc
            |   }
            |}
        """.trimIndent(), testConfiguration
        ) {
            documentablesTransformationStage = { module ->
                @Suppress("UNCHECKED_CAST") val funcs = module.children.single { it.name == "test" }
                    .children.single { it.name == "A" }
                    .children.filter { it.name == "ring" && it is DFunction } as List<DFunction>
                with(funcs) {
                    val sinceKotlin = mapOf(
                        Platform.jvm to SinceKotlinVersion("1.5"),
                    )

                    for(i in sinceKotlin) {
                        val tag =
                            find { it.sourceSets.first().analysisPlatform == i.key }?.documentation?.values?.first()
                                ?.dfs { it is CustomTagWrapper && it.name == "Since Kotlin" }
                                .assertNotNull("SinceKotlin[${i.key}]")
                        assertEquals((tag.children.first() as Text).body, i.value.toString())
                    }
                }
            }
        }
    }

    @Test
    fun `mpp fun without SinceKotlin annotation`() {
        val configuration =   dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/jvm/")
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    sourceRoots = listOf("src/native/")
                    analysisPlatform = "native"
                    name = "native"
                }
                sourceSet {
                    sourceRoots = listOf("src/common/")
                    analysisPlatform = "common"
                    name = "common"
                }
                sourceSet {
                    sourceRoots = listOf("src/js/")
                    analysisPlatform = "js"
                    name = "js"
                }
                sourceSet {
                    sourceRoots = listOf("src/wasm/")
                    analysisPlatform = "wasm"
                    name = "wasm"
                }
            }
        }
        testInline(
            """
            |/src/jvm/kotlin/test/source.kt
            |package test
            |
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
            |/src/native/kotlin/test/source.kt
            |package test
            |
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
            |/src/common/kotlin/test/source.kt
            |package test
            |
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
            |/src/js/kotlin/test/source.kt
            |package test
            |
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
            |/src/wasm/kotlin/test/source.kt
            |package test
            |
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
        """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                @Suppress("UNCHECKED_CAST") val funcs = module.children.single { it.name == "test" }
                    .children.filter { it.name == "ring" && it is DFunction } as List<DFunction>
                with(funcs) {
                    val sinceKotlin = mapOf(
                        Platform.common to SinceKotlinVersion("1.0"),
                        Platform.jvm to SinceKotlinVersion("1.0"),
                        Platform.js to SinceKotlinVersion("1.1"),
                        Platform.native to SinceKotlinVersion("1.3"),
                        Platform.wasm to SinceKotlinVersion("1.8"),
                    )

                    for(i in sinceKotlin) {
                        val tag =
                            find { it.sourceSets.first().analysisPlatform == i.key }?.documentation?.values?.first()
                                ?.dfs { it is CustomTagWrapper && it.name == "Since Kotlin" }
                                .assertNotNull("SinceKotlin[${i.key}]")
                        assertEquals((tag.children.first() as Text).body, i.value.toString())
                    }
                }
            }
        }
    }

    @Test
    fun `mpp fun with SinceKotlin annotation`() {
        val configuration =   dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/jvm/")
                    classpath = listOfNotNull(jvmStdlibPath)
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    sourceRoots = listOf("src/native/")
                    analysisPlatform = "native"
                    name = "native"
                }
                sourceSet {
                    sourceRoots = listOf("src/common/")
                    classpath = listOfNotNull(commonStdlibPath)
                    analysisPlatform = "common"
                    name = "common"
                }
                sourceSet {
                    sourceRoots = listOf("src/js/")
                    classpath = listOfNotNull(jsStdlibPath)
                    analysisPlatform = "js"
                    name = "js"
                }
                sourceSet {
                    sourceRoots = listOf("src/wasm/")
                    analysisPlatform = "wasm"
                    name = "wasm"
                }
            }
        }
        testInline(
            """
            |/src/jvm/kotlin/test/source.kt
            |package test
            |
            |/** dssdd */
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}            
            |/src/native/kotlin/test/source.kt
            |package test
            |
            |/** dssdd */
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
            |/src/common/kotlin/test/source.kt
            |package test
            |
            |/** dssdd */
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}            
            |/src/js/kotlin/test/source.kt
            |package test
            |
            |/** dssdd */
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}            
            |/src/wasm/kotlin/test/source.kt
            |package test
            |
            |/** dssdd */
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
        """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                @Suppress("UNCHECKED_CAST") val funcs = module.children.single { it.name == "test" }
                    .children.filter { it.name == "ring" && it is DFunction } as List<DFunction>
                with(funcs) {
                    val sinceKotlin = mapOf(
                        Platform.common to SinceKotlinVersion("1.3"),
                        Platform.jvm to SinceKotlinVersion("1.3"),
                        Platform.js to SinceKotlinVersion("1.3"),
                        Platform.native to SinceKotlinVersion("1.3"),
                        Platform.wasm to SinceKotlinVersion("1.8"),
                    )

                    for(i in sinceKotlin) {
                        val tag =
                            find { it.sourceSets.first().analysisPlatform == i.key }?.documentation?.values?.first()
                                ?.dfs { it is CustomTagWrapper && it.name == "Since Kotlin" }
                                .assertNotNull("SinceKotlin[${i.key}]")
                        assertEquals(i.value.toString(), (tag.children.first() as Text).body , "Platform ${i.key}")
                    }
                }
            }
        }
    }

    @Test
    fun `should do not render since kotlin tag when flag is unset`() {
        clearSystemProperty()
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |@SinceKotlin("1.3")
            |fun ring(abc: String): String {
            |    return "My precious " + abc
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "ring" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"ring" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "ring",
                                    "String",
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}
