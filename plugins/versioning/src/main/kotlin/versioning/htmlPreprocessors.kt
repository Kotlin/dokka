package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer

object MultiModuleStylesInstaller : PageTransformer {
    private val stylesPages = listOf(
        "styles/multimodule.css",
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(
            children = input.children + stylesPages.toRenderSpecificResourcePage()
        ).transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + stylesPages
            )
        }
}

private fun List<String>.toRenderSpecificResourcePage(): List<RendererSpecificResourcePage> =
    map { RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it")) }
