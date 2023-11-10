/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import kotlinx.html.FlowContent
import kotlinx.html.div
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.html.HtmlCodeBlockRenderer
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import signatures.renderedContent
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeBlocksTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `default code block rendering`() = testCode(
        """
            /src/test.kt
            package test
            
            /**
             * Hello, world!
             *
             * ```kotlin
             * test("hello kotlin")
             * ```
             *
             * ```custom
             * test("hello custom")
             * ```
             *
             * ```other
             * test("hello other")
             * ```
             */
            fun test(string: String) {}
            """.trimIndent(),
        emptyList()
    ) {
        val content = renderedContent("root/test/test.html")

        // by default, every code block is rendered as an element with `lang-XXX` class,
        //  where XXX=language of code block
        assertEquals(
            """test("hello kotlin")""",
            content.getElementsByClass("lang-kotlin").singleOrNull()?.wholeText()
        )
        assertEquals(
            """test("hello custom")""",
            content.getElementsByClass("lang-custom").singleOrNull()?.wholeText()
        )
        assertEquals(
            """test("hello other")""",
            content.getElementsByClass("lang-other").singleOrNull()?.wholeText()
        )
    }

    @Test
    fun `code block rendering with custom renderer`() = testCode(
        """
            /src/test.kt
            package test
            
            /**
             * Hello, world!
             *
             * ```kotlin
             * test("hello kotlin")
             * ```
             *
             * ```custom
             * test("hello custom")
             * ```
             *
             * ```other
             * test("hello other")
             * ```
             */
            fun test(string: String) {}
            """.trimIndent(),
        listOf(CustomPlugin(applyOtherRenderer = false)) // we add only one custom renderer
    ) {
        val content = renderedContent("root/test/test.html")
        assertEquals(
            """test("hello kotlin")""",
            content.getElementsByClass("lang-kotlin").singleOrNull()?.wholeText()
        )
        assertEquals(
            """test("hello custom")""",
            content.getElementsByClass("custom-language-block").singleOrNull()?.wholeText()
        )
        assertEquals(
            """test("hello other")""",
            content.getElementsByClass("lang-other").singleOrNull()?.wholeText()
        )
    }

    @Test
    fun `code block rendering with multiple custom renderers`() = testCode(
        """
            /src/test.kt
            package test
            
            /**
             * Hello, world!
             *
             * ```kotlin
             * test("hello kotlin")
             * ```
             *
             * ```custom
             * test("hello custom")
             * ```
             *
             * ```other
             * test("hello other")
             * ```
             */
            fun test(string: String) {}
            """.trimIndent(),
        listOf(CustomPlugin(applyOtherRenderer = true))
    ) {
        val content = renderedContent("root/test/test.html")
        assertEquals(
            """test("hello kotlin")""",
            content.getElementsByClass("lang-kotlin").singleOrNull()?.wholeText()
        )
        assertEquals(
            """test("hello custom")""",
            content.getElementsByClass("custom-language-block").singleOrNull()?.wholeText()
        )
        assertEquals(
            """test("hello other")""",
            content.getElementsByClass("other-language-block").singleOrNull()?.wholeText()
        )
    }

    @Test
    fun `multiline code block rendering with linebreaks`() = testCode(
        """
            /src/test.kt
            package test
            
            /**
             * Hello, world!
             *
             * ```kotlin
             * // something before linebreak
             *
             * test("hello kotlin")
             * ```
             *
             * ```custom
             * // something before linebreak
             *
             * test("hello custom")
             * ```
             *
             * ```other
             * // something before linebreak
             *
             * test("hello other")
             * ```
             */
            fun test(string: String) {}
            """.trimIndent(),
        listOf(CustomPlugin(applyOtherRenderer = false)) // we add only one custom renderer
    ) {
        val content = renderedContent("root/test/test.html")
        assertEquals(
            """
            // something before linebreak
            
            test("hello kotlin")
            """.trimIndent(),
            content.getElementsByClass("lang-kotlin").singleOrNull()?.wholeText()
        )
        assertEquals(
            """
            // something before linebreak
            
            test("hello custom")
            """.trimIndent(),
            content.getElementsByClass("custom-language-block").singleOrNull()?.wholeText()
        )
        assertEquals(
            """
            // something before linebreak
            
            test("hello other")
            """.trimIndent(),
            content.getElementsByClass("lang-other").singleOrNull()?.wholeText()
        )
    }

    private fun testCode(
        source: String,
        pluginOverrides: List<DokkaPlugin>,
        block: TestOutputWriter.() -> Unit
    ) {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(source, configuration, pluginOverrides = pluginOverrides + listOf(writerPlugin)) {
            renderingStage = { _, _ ->
                writerPlugin.writer.block()
            }
        }
    }

    private object CustomHtmlBlockRenderer : HtmlCodeBlockRenderer {
        override fun isApplicable(language: String): Boolean = language == "custom"

        override fun FlowContent.buildCodeBlock(language: String, code: String) {
            div("custom-language-block") {
                text(code)
            }
        }
    }

    private object CustomOtherHtmlBlockRenderer : HtmlCodeBlockRenderer {
        override fun isApplicable(language: String): Boolean = language == "other"

        override fun FlowContent.buildCodeBlock(language: String, code: String) {
            div("other-language-block") {
                text(code)
            }
        }
    }

    class CustomPlugin(applyOtherRenderer: Boolean) : DokkaPlugin() {
        val customHtmlBlockRenderer by extending {
            plugin<DokkaBase>().htmlCodeBlockRenderers with CustomHtmlBlockRenderer
        }

        val otherHtmlBlockRenderer by extending {
            plugin<DokkaBase>().htmlCodeBlockRenderers with CustomOtherHtmlBlockRenderer applyIf {
                applyOtherRenderer
            }
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }
}
