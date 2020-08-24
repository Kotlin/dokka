package org.jetbrains.dokka.newFrontend.renderer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.newFrontend.pages.ModulePageNode
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.Renderer

@ExperimentalSerializationApi
class NewFrontendRenderer(context: DokkaContext): Renderer {
    private val outputWriter = context.plugin<DokkaBase>().querySingle { outputWriter }

    override fun render(root: RootPageNode) {
        val page = root as ModulePageNode
        val bytes = Json.encodeToString(page)
        println("works")
        println(bytes)
        runBlocking(Dispatchers.Default) {
            launch {
                outputWriter.write("/sample", bytes, ".json")
            }
        }
    }
}

internal class MarkdownRenderer {
    fun render(contentNode: ContentNode): String {
        return when(contentNode){
            is ContentText -> buildText(contentNode)
            is ContentHeader -> buildHeader(contentNode)
            is ContentCodeBlock -> buildCodeBlock(contentNode)
            is ContentCodeInline -> buildCodeInline(contentNode)
            is ContentGroup -> buildGroup(contentNode)
            else -> {
                println("Failed to build ${contentNode.javaClass.canonicalName}")
                ""
            }
        }
    }

    fun render(contentNodes: List<ContentNode>): String = contentNodes.joinToString(separator = " ") { render(it) }

    private fun buildGroup(contentNode: ContentGroup): String = render(contentNode.children)


    private fun buildCodeInline(contentNode: ContentCodeInline): String = """`${render(contentNode.children)}`"""

    private fun buildCodeBlock(contentNode: ContentCodeBlock): String = """
        ```
        ${render(contentNode.children)}
        ```
    """.trimIndent()

    private fun buildHeader(contentNode: ContentHeader): String = "#".repeat(contentNode.level) + render(contentNode.children)

    private fun buildText(node: ContentText): String = node.text
}

object ContentNodeSerializer: KSerializer<ContentNode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContentNodeSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ContentNode = TODO("This should not happen")

    override fun serialize(encoder: Encoder, value: ContentNode) {
       encoder.encodeString(MarkdownRenderer().render(value))
    }
}