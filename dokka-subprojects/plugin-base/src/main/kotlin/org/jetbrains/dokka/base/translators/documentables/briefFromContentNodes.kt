/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.base.utils.firstNotNullOfOrNull
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*

public fun firstParagraphBrief(docTag: DocTag): DocTag? =
    when(docTag){
        is P -> docTag
        is CustomDocTag -> docTag.children.firstNotNullOfOrNull { firstParagraphBrief(it) }
        is Text -> docTag
        else -> null
    }

public fun firstSentenceBriefFromContentNodes(description: List<ContentNode>): List<ContentNode> {
    val firstSentenceRegex = """^((?:[^.?!]|[.!?](?!\s))*[.!?])""".toRegex()

    //Description that is entirely based on html content. In html it is hard to define a brief so we render all of it
    if(description.all { it.withDescendants().all { it is ContentGroup || (it as? ContentText)?.isHtml == true } }){
        return description
    }

    var sentenceFound = false
    fun lookthrough(node: ContentNode, neighbours: List<ContentNode>, currentIndex: Int): ContentNode =
        if (node.finishesWithSentenceNotFollowedByHtml(firstSentenceRegex, neighbours, currentIndex) || node.containsSentenceFinish(firstSentenceRegex)) {
            node as ContentText
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

private fun ContentNode.finishesWithSentenceNotFollowedByHtml(firstSentenceRegex: Regex, neighbours: List<ContentNode>, currentIndex: Int): Boolean =
    this is ContentText && !isHtml && matchContainsEnd(this, firstSentenceRegex) && !neighbours.nextElementIsHtml(currentIndex)

private fun ContentNode.containsSentenceFinish(firstSentenceRegex: Regex): Boolean =
    this is ContentText && !isHtml && firstSentenceRegex.containsMatchIn(text) && !matchContainsEnd(this, firstSentenceRegex)

private fun matchContainsEnd(node: ContentText, regex: Regex): Boolean =
    regex.find(node.text)?.let { node.text.endsWith(it.value) } ?: false

private fun List<ContentNode>.nextElementIsHtml(currentElementIndex: Int): Boolean =
    currentElementIndex != lastIndex && get(currentElementIndex + 1).isHtml

private val ContentNode.isHtml
    get() = extra[HtmlContent] != null
