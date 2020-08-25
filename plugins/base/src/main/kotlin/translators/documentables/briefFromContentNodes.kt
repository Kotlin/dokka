package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.TextStyle

fun briefFromContentNodes(description: List<ContentNode>): List<ContentNode> {
    val firstSentenceRegex = """^((?:[^.?!]|[.!?](?!\s))*[.!?])""".toRegex()

    var sentenceFound = false
    fun lookthrough(node: ContentNode): ContentNode =
        if (node is ContentText && firstSentenceRegex.containsMatchIn(node.text)) {
            sentenceFound = true
            node.copy(text = firstSentenceRegex.find(node.text)?.value.orEmpty())
        } else if (node is ContentGroup) {
            node.copy(children = node.children.mapNotNull {
                if (!sentenceFound) lookthrough(it) else null
            }, style = node.style - TextStyle.Paragraph)
        } else {
            node
        }
    return description.mapNotNull {
        if (!sentenceFound) lookthrough(it) else null
    }
}
