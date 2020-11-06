package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

enum class CopyButtonAttr(override val printableName: String?) : Style {
    TopRightPosition("top-right-position"),
    CopyIcon("copy-icon"),
    CopyPopupWrapper("copy-popup-wrapper popup-to-left"),
    CopyPopupIcon("copy-popup-icon")
}

open class CopyButtonAdder(val context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        input.transformContentPagesTree { page -> page.addCopyButtons() }

    protected open fun ContentPage.addCopyButtons(): ContentPage =
        modified(content = this.content.mapTransform<ContentGroup, ContentNode> { node ->
            if (node.dci.kind == ContentKind.Symbol && node.hasStyle(TextStyle.Monospace)) {
                ContentGroup(
                    node.children + copyButton("Content copied to clipboard", node),
                    node.dci,
                    node.sourceSets,
                    node.style,
                    node.extra
                )
            } else node
        })


//    protected open fun filterButtons(page: ContentPage, node: ContentNode): ContentGroup {
//        val buttons = page.content.withDescendants().flatMap { it.sourceSets }.distinct().map { dss ->
//            ContentButton(
//                label = dss.name,
//                dci = node.dci,
//                style = setOfNotNull(
//                    HtmlAttr.PlatformTag, HtmlAttr.PlatformSelector, dss.toHtmlClass()
//                ),
//                extra = PropertyContainer.withAll(
//                    SimpleAttr("data-active", "true"),
//                    SimpleAttr("data-filter", dss.sourceSetIDs.merged.toString())
//                ),
//                sourceSets = emptySet()
//            )
//        }
//
//        return ContentGroup(
//            children = buttons.toList(),
//            dci = node.dci,
//            style = setOf(HtmlAttr.FilterSection),
//            extra = PropertyContainer.withAll(SimpleAttr("id", "filter-section")),
//            sourceSets = node.sourceSets
//        )
//    }


//    private fun FlowContent.copyButton() = span(classes = "top-right-position") {
//        span("copy-icon")
//        copiedPopup("Content copied to clipboard", "popup-to-left")
//    }
//
//    private fun FlowContent.copiedPopup(notificationContent: String, additionalClasses: String = "") =
//        div("copy-popup-wrapper $additionalClasses") {
//            span("copy-popup-icon")
//            span {
//                text(notificationContent)
//            }
//        }

    private fun copyButton(notificationContent: String, node: ContentNode) = ContentGroup(
        children = listOf(
            ContentGroup(
                children = emptyList(),
                dci = node.dci,
                style = setOf(ContentStyle.Inline, CopyButtonAttr.CopyIcon),
                extra = PropertyContainer.empty(),
                sourceSets = node.sourceSets
            ),
            copiedPopup(notificationContent, node),
        ),
        dci = node.dci,
        style = setOf(ContentStyle.Inline, CopyButtonAttr.TopRightPosition),
        extra = PropertyContainer.empty(),
        sourceSets = node.sourceSets
    )

    private fun copiedPopup(notificationContent: String, node: ContentNode) =
        ContentGroup(
            children = listOf(
                ContentGroup(
                    children = emptyList(),
                    dci = node.dci,
                    style = setOf(ContentStyle.Inline, CopyButtonAttr.CopyPopupIcon),
                    extra = PropertyContainer.empty(),
                    sourceSets = node.sourceSets
                ),
                ContentGroup(
                    children = listOf(
                        ContentText(
                            text = notificationContent,
                            dci = node.dci,
                            style = emptySet(),
                            extra = PropertyContainer.empty(),
                            sourceSets = node.sourceSets
                        )
                    ),
                    dci = node.dci,
                    style = setOf(ContentStyle.Inline),
                    extra = PropertyContainer.empty(),
                    sourceSets = node.sourceSets
                ),
            ),
            dci = node.dci,
            style = setOf(CopyButtonAttr.CopyPopupWrapper),
            extra = PropertyContainer.empty(),
            sourceSets = node.sourceSets
        )

}
