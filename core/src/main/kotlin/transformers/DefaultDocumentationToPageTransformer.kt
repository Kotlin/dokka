package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.Model.Module
import org.jetbrains.dokka.pages.DefaultPageBuilder
import org.jetbrains.dokka.pages.DefaultPageContentBuilder
import org.jetbrains.dokka.pages.MarkdownToContentConverter
import org.jetbrains.dokka.pages.ModulePageNode


class DefaultDocumentationToPageTransformer(
    private val markdownConverter: MarkdownToContentConverter,
    private val logger: DokkaLogger
) : DocumentationToPageTransformer {
    override fun transform(module: Module): ModulePageNode =
        DefaultPageBuilder { node, kind, operation ->
            DefaultPageContentBuilder.group(node.dri, node.platformData, kind, markdownConverter, logger, operation)
        }.pageForModule(module)

}