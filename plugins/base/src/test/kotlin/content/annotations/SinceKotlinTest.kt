package content.annotations

import matchers.content.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer.Version
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.ContentPage
import org.junit.jupiter.api.Test
import utils.ParamAttributes
import utils.assertNotNull
import utils.bareSignature
import kotlin.test.assertEquals


class SinceKotlinTest : BaseAbstractTest() {

    val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun versionsComparing() {
        assert(Version("1.0").compareTo(Version("1.0")) == 0)
        assert(Version("1.0.0").compareTo(Version("1")) == 0)
        assert(Version("1.0") >= Version("1.0"))
        assert(Version("1.1") > Version("1"))
        assert(Version("1.0") < Version("2.0"))
        assert(Version("1.0") < Version("2.2"))
    }

    @Test
    fun `mpp fun without SinceKotlin annotation`() {
        val configuration =   dokkaConfiguration {
            extraOptions = listOf("-XXSinceKotlin")
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "native"
                }
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "common"
                }
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "js"
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/test/source.kt
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
                        Platform.common to Version("1.2"),
                        Platform.jvm to Version("1.0"),
                        Platform.js to Version("1.1"),
                        Platform.native to Version("1.3")
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
            extraOptions = listOf("-XXSinceKotlin")
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "native"
                }
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "common"
                }
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "js"
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/test/source.kt
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
                        Platform.common to Version("1.3"),
                        Platform.jvm to Version("1.3"),
                        Platform.js to Version("1.3"),
                        Platform.native to Version("1.3")
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
    fun `function with since kotlin annotation`() {
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
