package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import kotlin.test.assertEquals

class CoverPageTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(commonStdlibPath!!)
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    @Test
    fun `names of nested inheritors`() {
        val source = """
            |/src/main/kotlin/test/Test.kt
            |package example
            |
            | sealed class Result{
            |   class Success(): Result()
            |   class Failed(): Result()
            | }
            """
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.renderedContent("root/example/-result/index.html")
                val tableInheritors = content.select("div.table").single { it.previousElementSibling()?.text() == "Inheritors" && it.childrenSize() == 2 }
                assertEquals(tableInheritors.getElementsContainingOwnText("Failed").singleOrNull()?.tagName(), "a")
                assertEquals(tableInheritors.getElementsContainingOwnText("Success").singleOrNull()?.tagName(), "a")
            }
        }
    }
}