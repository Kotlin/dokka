package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

enum class HtmlAttr(override val printableName: String? = null) : Style {
    FilterSection("filter-section"), PlatformTag("platform-tag"), PlatformSelector("platform-selector"), CommonLike("common-like"), NativeLike("native-like"), JVMLike("jvm-like"), JSLike("js-like")
}

open class SourceSetFilteringButtonsAdder(val context: DokkaContext) : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode =
        if (shouldRenderSourceSetBubbles(input)) input.transformContentPagesTree { page -> page.addFilteringButtons() } else input


    protected open fun ContentPage.addFilteringButtons(): ContentPage =
        modified(content = this.content.mapTransform<ContentGroup, ContentNode> { node ->
            if (node.dci.kind == ContentKind.Cover) {
                ContentGroup(
                    listOf(filterButtons(this, node)) + node.children,
                    node.dci,
                    node.sourceSets,
                    node.style,
                    node.extra
                )
            } else node
        })


    protected open fun filterButtons(page: ContentPage, node: ContentNode): ContentGroup {
        val buttons = page.content.withDescendants().flatMap { it.sourceSets }.distinct().map { dss ->
            ContentButton(
                label = dss.name,
                dci = node.dci,
                style = setOfNotNull(
                    HtmlAttr.PlatformTag, HtmlAttr.PlatformSelector, dss.toHtmlClass()
                ),
                extra = PropertyContainer.withAll(
                    SimpleAttr("data-active", "true"),
                    SimpleAttr("data-filter", dss.sourceSetIDs.merged.toString())
                ),
                sourceSets = emptySet()
            )
        }

        return ContentGroup(
            children = buttons.toList(),
            dci = node.dci,
            style = setOf(HtmlAttr.FilterSection),
            extra = PropertyContainer.withAll(SimpleAttr("id", "filter-section")),
            sourceSets = node.sourceSets
        )
    }

    protected open fun DisplaySourceSet.toHtmlClass() = when (platform.key) {
        "common" -> HtmlAttr.CommonLike
        "native" -> HtmlAttr.NativeLike
        "jvm" -> HtmlAttr.JVMLike
        "js" -> HtmlAttr.JSLike
        else -> null
    }
}
