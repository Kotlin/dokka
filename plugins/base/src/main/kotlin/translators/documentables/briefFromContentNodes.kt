package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun firstParagraphBrief(docTag: DocTag): DocTag? =
    when(docTag){
        is P -> docTag
        is CustomDocTag -> docTag.children.firstNotNullResult { firstParagraphBrief(it) }
        is Text -> docTag
        else -> null
    }

fun firstSentenceBriefFromContentNodes(description: List<ContentNode>): List<ContentNode> {
    val firstSentenceRegex = """^((?:[^.?!]|[.!?](?!\s))*[.!?])""".toRegex()

    //Description that is entirely based on html content. In html it is hard to define a brief so we render all of it
    if(description.all { it.withDescendants().all { it is ContentGroup || it.safeAs<ContentText>()?.isHtml == true } }){
        return description
    }

    var sentenceFound = false
    fun lookthrough(node: ContentNode, neighbours: List<ContentNode>, currentIndex: Int): ContentNode =
        if (node is ContentText && !node.isHtml && firstSentenceRegex.containsMatchIn(node.text) && (currentIndex == neighbours.lastIndex || !neighbours[currentIndex + 1].isHtml)) {
            sentenceFound = true
            node.copy(text = firstSentenceRegex.find(node.text)?.value.orEmpty())
        } else if (node is ContentGroup) {
            node.copy(children = node.children.mapIndexedNotNull { i, element ->
                if (!sentenceFound) lookthrough(element, node.children, i) else null
            }, style = node.style - TextStyle.Paragraph)
        } else {
            node
        }
    return description.mapIndexedNotNull { i, element ->
        if (!sentenceFound) lookthrough(element, description, i) else null
    }
}

private val ContentNode.isHtml
    get() = extra[HtmlContent] != null
