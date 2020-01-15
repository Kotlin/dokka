package org.jetbrains.dokka

import org.jetbrains.dokka.pages.CommentsToContentConverter
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.postProcess.PostProcess
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.resolvers.LocationProvider
import org.jetbrains.dokka.transformers.descriptors.DescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentationNodeMerger
import org.jetbrains.dokka.transformers.documentation.DocumentationNodeTransformer
import org.jetbrains.dokka.transformers.documentation.DocumentationToPageTranslator
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer
import kotlin.reflect.KProperty


/**
 * Extension points declared by dokka core.
 * Default values are stored in [org.jetbrains.dokka.plugability.DefaultExtensions]
 */
object CoreExtensions {
    val descriptorToDocumentationTranslator by coreExtension<DescriptorToDocumentationTranslator>()
    val documentationMerger by coreExtension<DocumentationNodeMerger>()
    val documentationTransformer by coreExtension<DocumentationNodeTransformer>()
    val commentsToContentConverterFactory by coreExtension<(DokkaContext) -> CommentsToContentConverter>()
    val documentationToPageTranslator by coreExtension<DocumentationToPageTranslator>()
    val pageTransformer by coreExtension<PageNodeTransformer>()
    val rendererFactory by coreExtension<(FileWriter, LocationProvider, DokkaContext) -> Renderer>()
    val locationProviderFactory by coreExtension<(root: PageNode, DokkaConfiguration, DokkaContext) -> LocationProvider>()
    val fileExtension by coreExtension<String>()
    val postProcess by coreExtension<PostProcess>()

    private fun <T: Any> coreExtension() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }
}