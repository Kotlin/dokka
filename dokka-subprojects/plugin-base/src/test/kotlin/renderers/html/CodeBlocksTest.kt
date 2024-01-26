/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
import org.jsoup.nodes.Element
import signatures.renderedContent
import utils.TestOutputWriter
import utils.TestOutputWriterPlugin
import utils.assertContains
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CodeBlocksTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    private val contentWithExplicitLanguages =
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
        """.trimIndent()

    @Test
    fun `default code block rendering`() = testCode(
        contentWithExplicitLanguages,
        emptyList()
    ) {
        val content = renderedContent("root/test/test.html")

        // by default, every code block is rendered as an element with `lang-XXX` class,
        //  where XXX=language of code block
        assertEquals("""test("hello kotlin")""", content.textOfSingleElementByClass("lang-kotlin"))
        assertEquals("""test("hello custom")""", content.textOfSingleElementByClass("lang-custom"))
        assertEquals("""test("hello other")""", content.textOfSingleElementByClass("lang-other"))
    }

    @Test
    fun `code block rendering with custom renderer`() = testCode(
        contentWithExplicitLanguages,
        listOf(SingleRendererPlugin(CustomDefinedHtmlBlockRenderer))
    ) {
        val content = renderedContent("root/test/test.html")

        assertEquals("""test("hello kotlin")""", content.textOfSingleElementByClass("lang-kotlin"))
        assertEquals("""test("hello custom")""", content.textOfSingleElementByClass("custom-defined-language-block"))
        assertEquals("""test("hello other")""", content.textOfSingleElementByClass("lang-other"))
    }

    @Test
    fun `code block rendering with multiple custom renderers`() = testCode(
        contentWithExplicitLanguages,
        listOf(MultiRendererPlugin(CustomDefinedHtmlBlockRenderer, OtherDefinedHtmlBlockRenderer))
    ) {
        val content = renderedContent("root/test/test.html")

        assertEquals("""test("hello kotlin")""", content.textOfSingleElementByClass("lang-kotlin"))
        assertEquals("""test("hello custom")""", content.textOfSingleElementByClass("custom-defined-language-block"))
        assertEquals("""test("hello other")""", content.textOfSingleElementByClass("other-defined-language-block"))
    }

    private val contentWithImplicitLanguages =
        """
        /src/test.kt
        package test
        
        /**
         * Hello, world!
         *
         * ```
         * test("hello kotlin")
         * ```
         *
         * ```
         * test("hello custom")
         * ```
         *
         * ```
         * test("hello other")
         * ```
         */
        fun test(string: String) {}
        """.trimIndent()

    @Test
    fun `default code block rendering with undefined language`() = testCode(
        contentWithImplicitLanguages,
        emptyList()
    ) {
        val content = renderedContent("root/test/test.html")

        val contentsDefault = content.getElementsByClass("lang-kotlin").map(Element::wholeText)

        assertContains(contentsDefault, """test("hello kotlin")""")
        assertContains(contentsDefault, """test("hello custom")""")
        assertContains(contentsDefault, """test("hello other")""")

        assertEquals(3, contentsDefault.size)
    }

    @Test
    fun `code block rendering with custom renderer and undefined language`() = testCode(
        contentWithImplicitLanguages,
        listOf(SingleRendererPlugin(CustomUndefinedHtmlBlockRenderer))
    ) {
        val content = renderedContent("root/test/test.html")

        val contentsDefault = content.getElementsByClass("lang-kotlin").map(Element::wholeText)

        assertContains(contentsDefault, """test("hello kotlin")""")
        assertContains(contentsDefault, """test("hello other")""")

        assertEquals(2, contentsDefault.size)

        assertEquals("""test("hello custom")""", content.textOfSingleElementByClass("custom-undefined-language-block"))
    }

    @Test
    fun `code block rendering with multiple custom renderers and undefined language`() = testCode(
        contentWithImplicitLanguages,
        listOf(MultiRendererPlugin(CustomUndefinedHtmlBlockRenderer, OtherUndefinedHtmlBlockRenderer))
    ) {
        val content = renderedContent("root/test/test.html")

        assertEquals("""test("hello kotlin")""", content.textOfSingleElementByClass("lang-kotlin"))
        assertEquals("""test("hello custom")""", content.textOfSingleElementByClass("custom-undefined-language-block"))
        assertEquals("""test("hello other")""", content.textOfSingleElementByClass("other-undefined-language-block"))
    }

    @Test
    fun `code block rendering with multiple mixed custom renderers`() = testCode(
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
             * ```
             * test("hello custom")
             * ```
             *
             * ```other
             * test("hello other")
             * ```
             */
            fun test(string: String) {}
            """.trimIndent(),
        listOf(
            MultiRendererPlugin(
                CustomUndefinedHtmlBlockRenderer,
                OtherDefinedHtmlBlockRenderer,
            )
        )
    ) {
        val content = renderedContent("root/test/test.html")

        assertEquals("""test("hello kotlin")""", content.textOfSingleElementByClass("lang-kotlin"))
        assertEquals("""test("hello custom")""", content.textOfSingleElementByClass("custom-undefined-language-block"))
        assertEquals("""test("hello other")""", content.textOfSingleElementByClass("other-defined-language-block"))
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
             */
            fun test(string: String) {}
            """.trimIndent(),
        listOf(SingleRendererPlugin(CustomDefinedHtmlBlockRenderer))
    ) {
        val content = renderedContent("root/test/test.html")
        assertEquals(
            """
            // something before linebreak
            
            test("hello kotlin")
            """.trimIndent(),
            content.textOfSingleElementByClass("lang-kotlin")
        )
        assertEquals(
            """
            // something before linebreak
            
            test("hello custom")
            """.trimIndent(),
            content.textOfSingleElementByClass("custom-defined-language-block")
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

    private fun Element.textOfSingleElementByClass(className: String): String {
        val elements = getElementsByClass(className)
        assertEquals(1, elements.size)
        return elements.single().wholeText()
    }

    private object CustomDefinedHtmlBlockRenderer : HtmlCodeBlockRenderer {
        override fun isApplicableForDefinedLanguage(language: String): Boolean = language == "custom"
        override fun isApplicableForUndefinedLanguage(code: String): Boolean = false

        override fun FlowContent.buildCodeBlock(language: String?, code: String) {
            assertEquals("custom", language)
            div("custom-defined-language-block") {
                text(code)
            }
        }
    }

    private object OtherDefinedHtmlBlockRenderer : HtmlCodeBlockRenderer {
        override fun isApplicableForDefinedLanguage(language: String): Boolean = language == "other"
        override fun isApplicableForUndefinedLanguage(code: String): Boolean = false

        override fun FlowContent.buildCodeBlock(language: String?, code: String) {
            assertEquals("other", language)
            div("other-defined-language-block") {
                text(code)
            }
        }
    }

    private object CustomUndefinedHtmlBlockRenderer : HtmlCodeBlockRenderer {
        override fun isApplicableForDefinedLanguage(language: String): Boolean = false
        override fun isApplicableForUndefinedLanguage(code: String): Boolean = code.contains("custom")

        override fun FlowContent.buildCodeBlock(language: String?, code: String) {
            assertNull(language)
            div("custom-undefined-language-block") {
                text(code)
            }
        }
    }

    private object OtherUndefinedHtmlBlockRenderer : HtmlCodeBlockRenderer {
        override fun isApplicableForDefinedLanguage(language: String): Boolean = false
        override fun isApplicableForUndefinedLanguage(code: String): Boolean = code.contains("other")

        override fun FlowContent.buildCodeBlock(language: String?, code: String) {
            assertNull(language)
            div("other-undefined-language-block") {
                text(code)
            }
        }
    }

    class SingleRendererPlugin(renderer: HtmlCodeBlockRenderer) : DokkaPlugin() {
        val codeBlockRenderer by extending {
            plugin<DokkaBase>().htmlCodeBlockRenderers with renderer
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }

    class MultiRendererPlugin(
        renderer1: HtmlCodeBlockRenderer,
        renderer2: HtmlCodeBlockRenderer
    ) : DokkaPlugin() {
        val codeBlockRenderer1 by extending {
            plugin<DokkaBase>().htmlCodeBlockRenderers with renderer1
        }
        val codeBlockRenderer2 by extending {
            plugin<DokkaBase>().htmlCodeBlockRenderers with renderer2
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }
}
