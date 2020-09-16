package org.jetbrains.dokka

import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.plugability.LazyEvaluated
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import kotlin.reflect.KProperty

object CoreExtensions {
    val sourceToDocumentableTranslator by coreExtensionPoint<SourceToDocumentableTranslator>()
    val preMergeDocumentableTransformer by coreExtensionPoint<PreMergeDocumentableTransformer>()
    val documentableMerger by coreExtensionPoint<DocumentableMerger>()
    val documentableTransformer by coreExtensionPoint<DocumentableTransformer>()
    val documentableToPageTranslator by coreExtensionPoint<DocumentableToPageTranslator>()
    val allModulePageCreator by coreExtensionPoint<PageCreator>()
    val pageTransformer by coreExtensionPoint<PageTransformer>()
    val allModulePageTransformer by coreExtensionPoint<PageTransformer>()
    val renderer by coreExtensionPoint<Renderer>()

    private fun <T : Any> coreExtensionPoint() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }

    private fun <T: Any> coreExtension(extensionPoint: ExtensionPoint<T>, action: LazyEvaluated<T>) = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<Extension<T, OrderingKind.None, OverrideKind.None>> =
            lazy { Extension(extensionPoint, thisRef::class.qualifiedName!!, property.name, action) }
    }
}