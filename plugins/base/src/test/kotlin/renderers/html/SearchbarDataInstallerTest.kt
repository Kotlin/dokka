package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.Test
import org.junit.jupiter.api.Assertions
import utils.TestOutputWriterPlugin
import utils.pagesJson

class SearchbarDataInstallerTest: BaseAbstractTest() {

    @Test // see #2289
    fun `should display description of root declarations without a leading dot`() {
        val configuration = dokkaConfiguration {
            moduleName = "Dokka Module"

            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/kotlin/Test.kt")
                }
            }
        }

        val source = """
            |/src/kotlin/Test.kt
            |
            |class Test
            |
        """.trimIndent()

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val searchRecords = writerPlugin.writer.pagesJson()

                Assertions.assertEquals(
                    "Test",
                    searchRecords.find { record -> record.name == "class Test" }?.description ?: ""
                )
            }
        }
    }
}