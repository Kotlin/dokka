package org.jetbrains.dokka.javadoc.renderer

import org.jetbrains.dokka.javadoc.location.JavadocLocationProvider
import org.jetbrains.dokka.javadoc.pages.JavadocSignatureContentNode
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.formatToEndWithHtml
import org.jetbrains.dokka.utilities.htmlEscape

internal class JavadocContentToHtmlTranslator(
    private val locationProvider: JavadocLocationProvider,
    private val context: DokkaContext
) {

    fun htmlForContentNode(node: ContentNode, relative: PageNode?): String =
        when (node) {
            is ContentGroup -> htmlForContentNodes(node.children, node.style, relative)
            is ContentText -> buildText(node)
            is ContentDRILink -> buildLink(
                locationProvider.resolve(node.address, node.sourceSets, relative),
                htmlForContentNodes(node.children, node.style, relative)
            )
            is ContentResolvedLink -> buildLink(node.address, htmlForContentNodes(node.children, node.style, relative))
            is ContentCode -> htmlForCode(node.children)
            is JavadocSignatureContentNode -> htmlForSignature(node, relative)
            else -> ""
        }

    fun htmlForContentNodes(list: List<ContentNode>, styles: Set<Style>, relative: PageNode?) =
        list.joinToString(separator = "") { htmlForContentNode(it, relative) }

    private fun buildText(node: ContentText): String {
        val escapedText = node.text.htmlEscape()
        return if (node.style.contains(ContentStyle.InDocumentationAnchor)) {
            """<em><a id="$escapedText" class="searchTagResult">${escapedText}</a></em>"""
        } else {
            escapedText
        }
    }

    private fun htmlForCode(code: List<ContentNode>): String = code.map { element ->
        when (element) {
            is ContentText -> element.text
            is ContentBreakLine -> ""
            else -> run { context.logger.error("Cannot cast $element as ContentText!"); "" }
        }
    }.joinToString("<br>", """<span class="code">""", "</span>") { it }

    private fun htmlForSignature(node: JavadocSignatureContentNode, relative: PageNode?): String =
        listOfNotNull(
            node.annotations,
            node.modifiers,
            node.signatureWithoutModifiers,
            node.supertypes
        ).joinToString(separator = " ") { htmlForContentNode(it, relative) }

    companion object {

        fun buildLink(address: String, content: String) =
            """<a href=${address.formatToEndWithHtml()}>$content</a>"""

    }
}