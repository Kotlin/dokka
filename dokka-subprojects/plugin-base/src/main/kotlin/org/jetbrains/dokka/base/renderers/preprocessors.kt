/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.resolvers.shared.LinkFormat
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

public object RootCreator : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        RendererSpecificRootPage("", listOf(input), RenderingStrategy.DoNothing)
}

public class PackageListCreator(
    public val context: DokkaContext,
    public val format: LinkFormat,
    public val outputFilesNames: List<String> = listOf("package-list")
) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        return input.transformPageNodeTree { pageNode ->
            pageNode.takeIf { it is ModulePage }?.let { it.modified(children = it.children + packageList(input, it as ModulePage)) } ?: pageNode
        }
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
