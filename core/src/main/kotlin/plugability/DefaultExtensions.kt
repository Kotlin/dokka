package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.DocTagToContentConverter
import org.jetbrains.dokka.postProcess.DefaultPostProcess
import org.jetbrains.dokka.renderers.HtmlRenderer
import org.jetbrains.dokka.resolvers.DefaultLocationProvider
import org.jetbrains.dokka.transformers.descriptors.DefaultDescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationNodeMerger
import org.jetbrains.dokka.transformers.documentation.DefaultDocumentationToPageTranslator

object DefaultExtensions : DokkaExtensionHandler {
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun <T : Any, E : ExtensionPoint<T>> get(point: E, askDefault: AskDefault): List<T> =
        when (point) {
            CoreExtensions.descriptorToDocumentationTranslator -> DefaultDescriptorToDocumentationTranslator
            CoreExtensions.documentationMerger -> DefaultDocumentationNodeMerger
            CoreExtensions.commentsToContentConverterFactory -> ::DocTagToContentConverter
            CoreExtensions.documentationToPageTranslator -> DefaultDocumentationToPageTranslator
            CoreExtensions.rendererFactory -> ::HtmlRenderer
            CoreExtensions.locationProviderFactory -> ::DefaultLocationProvider
            CoreExtensions.fileExtension -> ".html"
            CoreExtensions.postProcess -> DefaultPostProcess
            else -> null
        }.let { listOfNotNull(it) as List<T> }
}