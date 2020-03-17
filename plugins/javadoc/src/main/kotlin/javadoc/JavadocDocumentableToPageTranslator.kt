package javadoc

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.DefaultPageBuilder
import org.jetbrains.dokka.pages.DefaultPageContentBuilder
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.transformers.documentation.DocumentationToPageTranslator

class JavadocDocumentableToPageTranslator : DocumentationToPageTranslator {
        override fun invoke(module: Module, context: DokkaContext): ModulePageNode =
            JavadocPageBuilder { node, kind, operation ->
                JavadocPageContentBuilder.group(
                    setOf(node.dri),
                    node.platformData,
                    kind,
                    context.single(CoreExtensions.commentsToContentConverter),
                    context.logger,
                    operation
                )
            }.pageForModule(module)
    }