package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.DocTagToContentConverter
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.renderers.HtmlRenderer
import org.jetbrains.dokka.resolvers.DefaultLocationProviderFactory
import org.jetbrains.dokka.transformers.descriptors.DefaultDescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationNodeMerger
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationToPageTranslator
import org.jetbrains.dokka.transformers.psi.DefaultPsiToDocumentationTranslator

internal object DefaultExtensions {

    private val renderer: LazyEvaluated<HtmlRenderer> = LazyEvaluated.fromRecipe { HtmlRenderer(it.single(CoreExtensions.outputWriter), it) }
    private val converter: LazyEvaluated<DocTagToContentConverter> = LazyEvaluated.fromRecipe {DocTagToContentConverter(it) }
    private val providerFactory: LazyEvaluated<DefaultLocationProviderFactory> = LazyEvaluated.fromRecipe { DefaultLocationProviderFactory(it) }


    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    internal fun <T : Any, E : ExtensionPoint<T>> get(point: E, fullContext: DokkaContext): List<T> =
        when (point) {
            CoreExtensions.descriptorToDocumentationTranslator ->  DefaultDescriptorToDocumentationTranslator
            CoreExtensions.psiToDocumentationTranslator -> DefaultPsiToDocumentationTranslator
            CoreExtensions.documentationMerger -> DefaultDocumentationNodeMerger
            CoreExtensions.commentsToContentConverter -> converter.get(fullContext)
            CoreExtensions.documentationToPageTranslator -> DefaultDocumentationToPageTranslator
            CoreExtensions.renderer -> renderer.get(fullContext)
            CoreExtensions.locationProviderFactory -> providerFactory.get(fullContext)
            CoreExtensions.outputWriter ->  FileWriter(fullContext.configuration.outputDir, "")
            CoreExtensions.fileExtension -> ".html"
            else -> null
        }.let { listOfNotNull( it ) as List<T> }
}