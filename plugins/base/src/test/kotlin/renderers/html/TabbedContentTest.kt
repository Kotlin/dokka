package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import signatures.renderedContent
import utils.TestOutputWriterPlugin
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
    private fun Element.getTable(type: String) = select("div[data-togglable=$type] .table")

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
                assertEquals(3, classContent.getTabbedRow("EXTENSION_FUNCTION").size)
                assertEquals(1, classContent.getTabbedRow("CONSTRUCTOR").size)
                assertEquals(1, classContent.getTable("PROPERTY").size)
                assertEquals(1, classContent.getTable("CONSTRUCTOR").size)
                assertEquals(1, classContent.getTable("FUNCTION").size)
                assertEquals(1, classContent.getTable("TYPE").size)
                assertEquals(2, classContent.getTabbedRow("EXTENSION_PROPERTY").size)

                val packagePage = writerPlugin.writer.renderedContent("root/example/index.html")
                assertEquals(1, packagePage.getTable("TYPE").size)
                assertEquals(1, packagePage.getTable("PROPERTY").size)
                assertEquals(1, packagePage.getTable("FUNCTION").size)
                assertEquals(3, packagePage.getTabbedRow("EXTENSION_FUNCTION").size)
                assertEquals(2, packagePage.getTabbedRow("EXTENSION_PROPERTY").size)
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
}