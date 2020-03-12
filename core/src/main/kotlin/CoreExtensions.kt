package org.jetbrains.dokka

import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.descriptors.DescriptorToDocumentableTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.psi.PsiToDocumentableTranslator
import kotlin.reflect.KProperty

object CoreExtensions {
    val descriptorToDocumentableTranslator by coreExtension<DescriptorToDocumentableTranslator>()
    val psiToDocumentableTranslator by coreExtension<PsiToDocumentableTranslator>()
    val preMergeDocumentableTransformer by coreExtension<PreMergeDocumentableTransformer>()
    val documentableMerger by coreExtension<DocumentableMerger>()
    val documentableTransformer by coreExtension<DocumentableTransformer>()
    val documentableToPageTranslator by coreExtension<DocumentableToPageTranslator>()
    val pageTransformer by coreExtension<PageTransformer>()
    val renderer by coreExtension<Renderer>()

    private fun <T : Any> coreExtension() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }
}