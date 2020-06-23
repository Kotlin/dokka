package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.base.plugability.DokkaContext
import org.jetbrains.dokka.base.transformers.pages.PageTransformer

object RootCreator : PageTransformer {
    override fun invoke(input: RootPageNode) =
        RendererSpecificRootPage("", listOf(input), RenderingStrategy.DoNothing)
}


class PackageListCreator(val context: DokkaContext, val format: String, val linkExtension: String) : PageTransformer {
    override fun invoke(input: RootPageNode) =
        input.modified(children = input.children.map {
            it.takeUnless { it is ModulePageNode }
                ?: it.modified(children = it.children + packageList(input)) // TODO packageList should take module as an input
        })


    private fun packageList(pageNode: RootPageNode) =
        RendererSpecificResourcePage(
            "${pageNode.name}/package-list",
            emptyList(),
            RenderingStrategy.Write(PackageListService(context).formatPackageList(pageNode, format, linkExtension))
        )
}
