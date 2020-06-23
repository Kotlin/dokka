package org.jetbrains.dokka.base

import org.jetbrains.dokka.base.plugability.ExtensionPoint
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.base.transformers.documentables.DocumentableMerger
import org.jetbrains.dokka.base.transformers.documentables.DocumentableToPageTranslator
import org.jetbrains.dokka.base.transformers.documentables.DocumentableTransformer
import org.jetbrains.dokka.base.transformers.documentables.PreMergeDocumentableTransformer
import org.jetbrains.dokka.base.transformers.pages.PageCreator
import org.jetbrains.dokka.base.transformers.pages.PageTransformer
import org.jetbrains.dokka.base.transformers.sources.SourceToDocumentableTranslator
import kotlin.reflect.KProperty

// TODO NOW: rename to BaseExtensions?
object CoreExtensions {
    val sourceToDocumentableTranslator by coreExtension<SourceToDocumentableTranslator>()
    val preMergeDocumentableTransformer by coreExtension<PreMergeDocumentableTransformer>()
    val documentableMerger by coreExtension<DocumentableMerger>()
    val documentableTransformer by coreExtension<DocumentableTransformer>()
    val documentableToPageTranslator by coreExtension<DocumentableToPageTranslator>()
    val allModulePageCreator by coreExtension<PageCreator>()
    val pageTransformer by coreExtension<PageTransformer>()
    val allModulePageTransformer by coreExtension<PageTransformer>()
    val renderer by coreExtension<Renderer>()

    private fun <T : Any> coreExtension() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }
}