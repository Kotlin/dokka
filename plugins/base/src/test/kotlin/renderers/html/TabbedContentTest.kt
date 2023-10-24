/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.nodes.Element
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class TabbedContentTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    private fun Element.getTabbedRow(type: String) = select(".table-row[data-togglable=$type]")
    private fun Element.getTabbedTable(type: String) = select("div[data-togglable=$type] .table")
    private fun Element.getMainContentDataType() = selectFirst(".main-content")?.attr("data-page-type")

    @Test
    fun `should have correct tabbed content type`() {
        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            |val p = 0
            |fun foo() = 0
            |
            | class A(val d: Int = 0) {
            |   class Success(): Result()
            |   class Failed(): Result()
            |   
            |   fun fn() = 0
            | }
            | 
            | fun A.fn() = 0
            | fun A.fn2() = 0
            | fun A.fn3() = 0
            | val A.p = 0
            | val A.p2 = 0
            """
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val classContent = writerPlugin.writer.renderedContent("root/example/-a/index.html")
                assertEquals(1, classContent.getTabbedTable("CONSTRUCTOR").size)
                assertEquals(1, classContent.getTabbedTable("PROPERTY").size)
                assertEquals(1, classContent.getTabbedTable("CONSTRUCTOR").size)
                assertEquals(1, classContent.getTabbedTable("FUNCTION").size)
                assertEquals(1, classContent.getTabbedTable("TYPE").size)
                assertEquals(3, classContent.getTabbedRow("EXTENSION_FUNCTION").size)
                assertEquals(2, classContent.getTabbedRow("EXTENSION_PROPERTY").size)
                assertEquals("classlike", classContent.getMainContentDataType())

                val packagePage = writerPlugin.writer.renderedContent("root/example/index.html")
                assertEquals(1, packagePage.getTabbedTable("TYPE").size)
                assertEquals(1, packagePage.getTabbedTable("PROPERTY").size)
                assertEquals(1, packagePage.getTabbedTable("FUNCTION").size)
                assertEquals(3, packagePage.getTabbedRow("EXTENSION_FUNCTION").size)
                assertEquals(2, packagePage.getTabbedRow("EXTENSION_PROPERTY").size)
                assertEquals("package", packagePage.getMainContentDataType())
            }
        }
    }

    @Test
    fun `should not have Types-tab where there are not types`() {
        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            |val p = 0
            |fun foo() = 0
            |
            |/src/main/kotlin/test/PackageTwo.kt
            |package example2
            |
            |class A
            """
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val packagePage = writerPlugin.writer.renderedContent("root/example/index.html")
                assertEquals(0, packagePage.select("*[data-togglable=TYPE]").size)
                assertEquals(1, packagePage.getTabbedTable("PROPERTY").size)
                assertEquals(1, packagePage.getTabbedTable("FUNCTION").size)

                val packagePage2 = writerPlugin.writer.renderedContent("root/example2/index.html")
                assertEquals(2, packagePage2.select("*[data-togglable=TYPE]").size)
                assertEquals(0, packagePage2.getTabbedTable("PROPERTY").size)
                assertEquals(0, packagePage2.getTabbedTable("FUNCTION").size)
            }
        }
    }

    @Test
    fun `should have correct order of members and extensions`() {
        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            |val p = 0
            |fun foo() = 0
            |
            |class A(val d: Int = 0) {
            |   fun fn() = 0
            |   fun a() = 0
            |   fun g() = 0
            |}
            | 
            | fun A.fn() = 0
            """
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val classContent = writerPlugin.writer.renderedContent("root/example/-a/index.html")
                val funTable = classContent.select("div[data-togglable=FUNCTION] .table")
                val orders =
                    funTable.select(".table-row").map { it.attr("data-togglable") }
                assertEquals(listOf("", "", "EXTENSION_FUNCTION", ""), orders)
                val names =
                    funTable.select(".main-subrow .inline-flex a").map { it.text() }
                assertEquals(listOf("a", "fn", "fn", "g"), names)
            }
        }
    }

    @Test
    fun `should have expected order of content types within a members tab`() {
        val source = """
            |/src/main/kotlin/test/Result.kt
            |package example
            |
            |class Result(val d: Int = 0) {
            |  class Success(): Result()
            |  
            |  val isFailed = false
            |  fun reset() = 0
            |  fun String.extension() = 0
            |}
            """
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val classContent = writerPlugin.writer.renderedContent("root/example/-result/index.html")
                val tabSectionNames = classContent.select("div .tabs-section-body > div[data-togglable]")
                    .map { it.attr("data-togglable") }

                val expectedOrder = listOf("CONSTRUCTOR", "TYPE", "PROPERTY", "FUNCTION")

                assertEquals(expectedOrder.size, tabSectionNames.size)
                expectedOrder.forEachIndexed { index, element ->
                    assertEquals(element, tabSectionNames[index])
                }
            }
        }
    }
}
