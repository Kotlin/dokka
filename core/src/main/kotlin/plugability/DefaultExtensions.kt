package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.DocTagToContentConverter
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.renderers.HtmlRenderer
import org.jetbrains.dokka.resolvers.DefaultLocationProviderFactory
import org.jetbrains.dokka.transformers.descriptors.DefaultDescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationNodeMerger
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationToPageTranslator

internal object DefaultExtensions {
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    internal fun <T : Any, E : ExtensionPoint<T>> get(point: E, fullContext: DokkaContext): List<T> =
        when (point) {
            CoreExtensions.descriptorToDocumentationTranslator ->  DefaultDescriptorToDocumentationTranslator
            CoreExtensions.documentationMerger -> DefaultDocumentationNodeMerger
            CoreExtensions.commentsToContentConverter -> DocTagToContentConverter(fullContext)
            CoreExtensions.documentationToPageTranslator -> DefaultDocumentationToPageTranslator
            CoreExtensions.renderer -> HtmlRenderer(fullContext.single(CoreExtensions.outputWriter), fullContext)
            CoreExtensions.locationProviderFactory -> DefaultLocationProviderFactory
            CoreExtensions.outputWriter ->  FileWriter(fullContext.configuration.outputDir, "")
            CoreExtensions.fileExtension -> ".html"
            else -> null
        }.let { listOfNotNull( it ) as List<T> }
}