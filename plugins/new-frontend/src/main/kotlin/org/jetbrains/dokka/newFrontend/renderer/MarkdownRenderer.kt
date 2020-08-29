package org.jetbrains.dokka.newFrontend.renderer

import org.jetbrains.dokka.pages.*

object MarkdownRenderer {
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