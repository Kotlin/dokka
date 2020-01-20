package org.jetbrains.dokka

import org.jetbrains.dokka.pages.CommentsToContentConverter
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.renderers.OutputWriter
import org.jetbrains.dokka.resolvers.LocationProviderFactory
import org.jetbrains.dokka.transformers.descriptors.DescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentationNodeMerger
import org.jetbrains.dokka.transformers.documentation.DocumentationNodeTransformer
import org.jetbrains.dokka.transformers.documentation.DocumentationToPageTranslator
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer
import org.jetbrains.dokka.transformers.psi.PsiToDocumentationTranslator
import kotlin.reflect.KProperty


/**
 * Extension points declared by dokka core.
 * Default values are stored in [org.jetbrains.dokka.plugability.DefaultExtensions]
 */
object CoreExtensions {
    val descriptorToDocumentationTranslator by coreExtension<DescriptorToDocumentationTranslator>()
    val psiToDocumentationTranslator by coreExtension<PsiToDocumentationTranslator>()
    val documentationMerger by coreExtension<DocumentationNodeMerger>()
    val documentationTransformer by coreExtension<DocumentationNodeTransformer>()
    val commentsToContentConverter by coreExtension<CommentsToContentConverter>()
    val documentationToPageTranslator by coreExtension<DocumentationToPageTranslator>()
    val pageTransformer by coreExtension<PageNodeTransformer>()
    val locationProviderFactory by coreExtension<LocationProviderFactory>()
    val outputWriter by coreExtension<OutputWriter>()
    val renderer by coreExtension<Renderer>()
    val fileExtension by coreExtension<String>()

    private fun <T: Any> coreExtension() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }
}