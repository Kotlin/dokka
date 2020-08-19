package org.jetbrains.dokka.javadoc.renderer

import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.javadoc.pages.JavadocSignatureContentNode
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.formatToEndWithHtml
import org.jetbrains.dokka.utilities.htmlEscape

internal class JavadocContentToHtmlTranslator(
    private val locationProvider: LocationProvider,
    private val context: DokkaContext
) {

    fun htmlForContentNode(node: ContentNode, relative: PageNode?): String =
        when (node) {
            is ContentGroup ->
                if (node.style.contains(TextStyle.Paragraph)) htmlForParagraph(node.children, relative)
                else htmlForContentNodes(node.children, relative)
            is ContentText -> htmlForText(node)
            is ContentDRILink -> buildLinkFromNode(node, relative)
            is ContentResolvedLink -> buildLinkFromNode(node, relative)
            is ContentCode -> htmlForCode(node, relative)
            is ContentList -> htmlForList(node.children, relative)
            is JavadocSignatureContentNode -> htmlForSignature(node, relative)
            is ContentBreakLine -> "<br>"
            else -> ""
        }

    fun htmlForText(node: ContentText): String {
        val escapedText = node.text.htmlEscape()
        return when {
            node.style.contains(ContentStyle.InDocumentationAnchor) -> """<em><a id="$escapedText" class="searchTagResult">${escapedText}</a></em>"""
            node.style.contains(TextStyle.Bold) -> "<b>$escapedText</b>"
            node.style.contains(TextStyle.Italic) -> "<i>$escapedText</i>"
            node.style.contains(TextStyle.Strikethrough) -> "<del>$escapedText</del>"
            else -> node.text.htmlEscape()
        }
    }

    fun htmlForContentNodes(list: List<ContentNode>, relative: PageNode?) =
        list.joinToString(separator = "") { htmlForContentNode(it, relative) }

    private fun htmlForParagraph(nodes: List<ContentNode>, relative: PageNode?) =
        "<p>${htmlForContentNodes(nodes, relative)}</p>"

    private fun htmlForCode(code: ContentCode, relative: PageNode?): String {
        fun nodeToText(node: ContentNode, insidePre: Boolean = false): String = when (node) {
            is ContentText -> node.text.htmlEscape()
            is ContentBreakLine -> if(insidePre) "\n" else "<br>"
            is ContentDRILink -> buildLinkFromNode(node, relative)
            is ContentResolvedLink -> buildLinkFromNode(node, relative)
            is ContentCodeBlock -> "<pre><code>${node.children.joinToString("") { nodeToText(it, insidePre = true) }}</code></pre>"
            is ContentCodeInline -> "<code>${node.children.joinToString("") { nodeToText(it) }}</code>"
            else -> run { context.logger.error("Cannot cast $node as ContentText!"); "" }
        }
        return nodeToText(code)
    }

    private fun htmlForList(elements: List<ContentNode>, relative: PageNode?) =
        elements.filterIsInstance<ContentGroup>()
            .joinToString("", "<ul>", "</ul>") { "<li>${htmlForContentNode(it, relative)}</li>" }

    private fun htmlForSignature(node: JavadocSignatureContentNode, relative: PageNode?): String =
        listOfNotNull(
            node.annotations,
            node.modifiers,
            node.signatureWithoutModifiers,
            node.supertypes
        ).joinToString(separator = " ") { htmlForContentNode(it, relative) }

    private fun buildLinkFromNode(node: ContentDRILink, relative: PageNode?) =
        locationProvider.resolve(node.address, node.sourceSets, relative)?.let {
            buildLink(it, htmlForContentNodes(node.children, relative))
        } ?: htmlForContentNodes(node.children, relative)

    private fun buildLinkFromNode(node: ContentResolvedLink, relative: PageNode?) =
        buildLink(node.address, htmlForContentNodes(node.children, relative))

    companion object {

        fun buildLink(address: String, content: String) =
            """<a href=${address.formatToEndWithHtml()}>$content</a>"""

    }
}