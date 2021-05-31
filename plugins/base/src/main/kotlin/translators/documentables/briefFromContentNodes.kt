package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun briefFromContentNodes(description: List<ContentNode>): List<ContentNode> {
    val firstSentenceRegex = """^((?:[^.?!]|[.!?](?!\s))*[.!?])""".toRegex()

    //Description that is entirely based on html content. In html it is hard to define a brief so we render all of it
    if(description.all { it.withDescendants().all { it is ContentGroup || it.safeAs<ContentText>()?.isHtml == true } }){
        return description
    }

    var sentenceFound = false
    fun lookthrough(node: ContentNode): ContentNode =
        if (node is ContentText && !node.isHtml && firstSentenceRegex.containsMatchIn(node.text)) {
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

private val ContentText.isHtml
    get() = extra[HtmlContent] != null
