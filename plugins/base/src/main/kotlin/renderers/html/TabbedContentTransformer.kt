package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.TabSortingStrategy
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer

enum class HtmlStyle(override val printableName: String) : Style {
    SectionTab("section-tab")
}

open class TabbedContentTransformer(val context: DokkaContext) : PageTransformer {
    private val tabSortingStrategy = context.plugin<DokkaBase>().querySingle { tabSortingStrategy }

    override fun invoke(input: RootPageNode): RootPageNode =
        input.transformContentPagesTree { page -> page.transformTabbedContent() }


    protected open fun ContentPage.transformTabbedContent(): ContentPage =
        modified(content = this.content.mapTransform<ContentGroup, ContentNode> { node ->
            if (node.hasStyle(ContentStyle.TabbedContent)) {
                val secondLevel = node.children.filterIsInstance<ContentComposite>().flatMap { it.children }
                    .filterIsInstance<ContentHeader>().flatMap { it.children }.filterIsInstance<ContentText>()
                val firstLevel = node.children.filterIsInstance<ContentHeader>().flatMap { it.children }
                    .filterIsInstance<ContentText>()

                val renderable = sortTabs(tabSortingStrategy, firstLevel.union(secondLevel))

                val buttons = renderable.mapIndexed { index, renderableNode ->
                    ContentButton(
                        label = renderableNode.text,
                        dci = node.dci,
                        style = setOf(HtmlStyle.SectionTab),
                        extra = PropertyContainer.withAll(
                            SimpleAttr("data-togglable", renderableNode.text),
                            SimpleAttr("data-active", "true").takeIf { index == 0 }),
                        sourceSets = renderableNode.sourceSets
                    )
                }
                val buttonsGroup = ContentGroup(
                    buttons,
                    dci = node.dci,
                    style = setOf(ContentClass.TabsSection),
                    extra = PropertyContainer.withAll(SimpleAttr("tabs-section", "tabs-section")),
                    sourceSets = node.sourceSets
                )
                val contentGroup = ContentGroup(
                    node.children,
                    dci = node.dci,
                    style = node.style + setOf(ContentClass.TabsSectionBody),
                    extra = node.extra,
                    sourceSets = node.sourceSets
                )

                ContentGroup(
                    listOf(buttonsGroup, contentGroup),
                    dci = node.dci,
                    style = node.style + setOf(TextStyle.Block),
                    extra = node.extra,
                    sourceSets = node.sourceSets
                )
            } else node

        }
        )


    private fun <T : ContentNode> sortTabs(strategy: TabSortingStrategy, tabs: Collection<T>): List<T> {
        val sorted = strategy.sort(tabs)
        if (sorted.size != tabs.size)
            context.logger.warn("Tab sorting strategy has changed number of tabs from ${tabs.size} to ${sorted.size}")
        return sorted;
    }
}
