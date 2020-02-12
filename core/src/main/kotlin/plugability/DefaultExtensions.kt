package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.DocTagToContentConverter
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.renderers.OutputWriter
import org.jetbrains.dokka.renderers.html.HtmlRenderer
import org.jetbrains.dokka.resolvers.DefaultLocationProviderFactory
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationNodeMerger
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationToPageTranslator
import org.jetbrains.dokka.transformers.pages.DefaultPageMergerStrategy
import org.jetbrains.dokka.transformers.psi.DefaultPsiToDocumentationTranslator
import org.jetbrains.dokka.transformers.pages.DefaultPageNodeMerger

internal object DefaultExtensions {

    private val renderer: LazyEvaluated<HtmlRenderer> = LazyEvaluated.fromRecipe {
        HtmlRenderer(
            it.single(CoreExtensions.outputWriter),
            it
        )
    }
    private val converter: LazyEvaluated<DocTagToContentConverter> = LazyEvaluated.fromRecipe { DocTagToContentConverter(it) }
    private val providerFactory: LazyEvaluated<DefaultLocationProviderFactory> = LazyEvaluated.fromRecipe { DefaultLocationProviderFactory(it) }
    private val outputWriter: LazyEvaluated<OutputWriter> = LazyEvaluated.fromRecipe { FileWriter(it) }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    internal fun <T : Any, E : ExtensionPoint<T>> get(point: E, fullContext: DokkaContext): List<T> =
        when (point) {
            CoreExtensions.psiToDocumentationTranslator -> DefaultPsiToDocumentationTranslator
            CoreExtensions.documentationMerger -> DefaultDocumentationNodeMerger
            CoreExtensions.commentsToContentConverter -> converter.get(fullContext)
            CoreExtensions.documentationToPageTranslator -> DefaultDocumentationToPageTranslator
            CoreExtensions.pageTransformer -> DefaultPageNodeMerger(fullContext)
            CoreExtensions.renderer -> renderer.get(fullContext)
            CoreExtensions.locationProviderFactory -> providerFactory.get(fullContext)
            CoreExtensions.outputWriter ->  outputWriter.get(fullContext)
            CoreExtensions.pageMergerStrategy -> DefaultPageMergerStrategy(fullContext.logger)
            else -> null
        }.let { listOfNotNull( it ) as List<T> }
}