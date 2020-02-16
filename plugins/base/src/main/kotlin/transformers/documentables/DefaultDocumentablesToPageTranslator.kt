package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentablesToPageTranslator


object DefaultDocumentablesToPageTranslator : DocumentablesToPageTranslator {
    override fun invoke(module: Module, context: DokkaContext): ModulePageNode =
        DefaultPageBuilder { node, kind, operation ->
            DefaultPageContentBuilder.group(
                setOf(node.dri),
                node.platformData,
                kind,
                context.single(CoreExtensions.commentsToContentConverter),
                context.logger,
                operation
            )
        }.pageForModule(module)
}