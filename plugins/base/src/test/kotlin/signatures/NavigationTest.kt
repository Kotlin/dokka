package signatures

import org.jetbrains.dokka.base.renderers.html.NavigationNode
import org.jetbrains.dokka.base.templating.parseJson
import org.jetbrains.dokka.model.withDescendants
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import kotlin.test.assertEquals

class NavigationTest : AbstractRenderingTest() {
    @Test
    fun `id-s should be unique`() {
        val writerPlugin = TestOutputWriterPlugin()

        testFromData(
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.contents.getValue("scripts/navigation.js")
                assert(content.isNotBlank() && content.isNotEmpty())
                assert(content.split("=").size == 2)
                val json = content.split("=")[1]

                val parsedContent: List<NavigationNode> = parseJson(json)
                val duplicates = parsedContent[0].withDescendants()
                    .fold(emptySet<String>() to emptyList<String>()) { acc, navigationNode ->
                        if (navigationNode.id in acc.first) {
                            acc.first to acc.second + navigationNode.id
                        } else {
                            acc.first + navigationNode.id to acc.second
                        }
                    }.second

                assertEquals(
                    0,
                    duplicates.size,
                    "Assumed ids are distinct, but found duplicates: ${duplicates.joinToString()}"
                )
            }
        }
    }
}