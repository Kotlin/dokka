package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.resolvers.shared.LinkFormat
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

object RootCreator : PageTransformer {
    override fun invoke(input: RootPageNode) =
        RendererSpecificRootPage("", listOf(input), RenderingStrategy.DoNothing)
}

class PackageListCreator(
    val context: DokkaContext,
    val format: LinkFormat,
    val outputFilesNames: List<String> = listOf("package-list")
) : PageTransformer {
    override fun invoke(input: RootPageNode) = input.transformPageNodeTree { pageNode ->
            pageNode.takeIf { it is ModulePage }?.let { it.modified(children = it.children + packageList(input, it as ModulePage)) } ?: pageNode
        }

    private fun packageList(rootPageNode: RootPageNode, module: ModulePage): List<RendererSpecificPage> {
        val content = PackageListService(context, rootPageNode).createPackageList(
            module,
            format
        )
        return outputFilesNames.map { fileName ->
            RendererSpecificResourcePage(
                fileName,
                emptyList(),
                RenderingStrategy.Write(content)
            )
        }
    }
}
