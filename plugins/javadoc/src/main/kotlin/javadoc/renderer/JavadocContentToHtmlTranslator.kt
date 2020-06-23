package javadoc.renderer

import javadoc.pages.TextNode
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.nio.file.Path
import java.nio.file.Paths

internal class JavadocContentToHtmlTranslator(
    private val locationProvider: LocationProvider,
    private val context: DokkaContext
) {

    fun <T> htmlForContentNode(node: ContentNode, relative: T?, locate: ContentDRILink.(T?) -> String): String =
        when (node) {
            is ContentGroup -> htmlForContentNodes(node.children, relative, locate)
            is ContentText -> node.text
            is TextNode -> node.text
            is ContentDRILink -> buildLink(
                node.locate(relative),
                htmlForContentNodes(node.children, relative, locate)
            )
            is ContentResolvedLink -> buildLink(node.address, htmlForContentNodes(node.children, relative, locate))
            is ContentCode -> htmlForCode(node.children)
            else -> ""
        }

    fun <T> htmlForContentNodes(list: List<ContentNode>, relative: T?, locate: ContentDRILink.(T?) -> String) =
        list.joinToString(separator = "") { htmlForContentNode(it, relative, locate) }

    private fun locate(link: ContentDRILink, relativePath: String?) =
        resolveLink(link.address, link.sourceSets, relativePath)

    fun htmlForContentNodes(list: List<ContentNode>, relative: String?) =
        htmlForContentNodes(list, relative, ::locate)

    private fun htmlForCode(code: List<ContentNode>): String = code.map { element ->
        when (element) {
            is ContentText -> element.text
            is ContentBreakLine -> ""
            else -> run { context.logger.error("Cannot cast $element as ContentText!"); "" }
        }
    }.joinToString("<br>", """<span class="code">""", "</span>") { it }

    private fun resolveLink(address: DRI, sourceSets: Set<DokkaConfiguration.DokkaSourceSet>, relativePath: String?) =
        locationProvider.resolve(address, sourceSets).let {
            val afterFormattingToHtml = it.formatToEndWithHtml()
            if (relativePath != null) afterFormattingToHtml.relativizePath(relativePath)
            else afterFormattingToHtml
        }

    private fun String.relativizePath(parent: String) =
        Paths.get(parent).relativize(Paths.get(this)).normalize().toFile().toString()

    companion object {

        fun buildLink(address: String, content: String) =
            """<a href=${address.formatToEndWithHtml()}>$content</a>"""

        private fun String.formatToEndWithHtml() =
            if (endsWith(".html")) this else "$this.html"
    }
}