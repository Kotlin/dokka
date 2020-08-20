package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.resolvers.shared.LinkFormat
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

object RootCreator : PageTransformer {
    override fun invoke(input: RootPageNode) =
        RendererSpecificRootPage("", listOf(input), RenderingStrategy.DoNothing)
}


class PackageListCreator(val context: DokkaContext, val format: LinkFormat, val outputFilesNames: List<String> = listOf("package-list")) : PageTransformer {
    override fun invoke(input: RootPageNode) =
        input.modified(children = input.children.map {
            it.takeUnless { it is ModulePage }
                ?: it.modified(children = it.children + packageList(input)) // TODO packageList should take module as an input
        })


    private fun packageList(pageNode: RootPageNode): List<RendererSpecificPage> {
        val content = PackageListService(context).formatPackageList(pageNode, format.formatName, format.linkExtension)
        return outputFilesNames.map { fileName ->
            RendererSpecificResourcePage(
                "${pageNode.name}/${fileName}",
                emptyList(),
                RenderingStrategy.Write(content)
            )
        }
    }
}
