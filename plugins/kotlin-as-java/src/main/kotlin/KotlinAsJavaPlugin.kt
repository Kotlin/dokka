package  org.jetbrains.dokka.kotlinAsJava


import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.transformers.documentation.DocumentationToPageTranslator
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDescriptorToDocumentableTranslator by extending {
        CoreExtensions.descriptorToDocumentationTranslator providing ::KotlinAsJavaDescriptorToDocumentationTranslator
    }
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        CoreExtensions.documentationToPageTranslator with KotlinAsJavaDocumentationToPageTranslator
    }
}

object DescriptorCache {
    private val cache: HashMap<DRI, DeclarationDescriptor> = HashMap()

    fun add(dri: DRI, descriptor: DeclarationDescriptor): Boolean = cache.putIfAbsent(dri, descriptor) == null
    operator fun get(dri: DRI): DeclarationDescriptor? = cache[dri]
}

object KotlinAsJavaDocumentationToPageTranslator : DocumentationToPageTranslator {
    override fun invoke(module: Module, context: DokkaContext): ModulePageNode =
        KotlinAsJavaPageBuilder { node, kind, operation ->
            KotlinAsJavaPageContentBuilder.group(
                setOf(node.dri),
                node.platformData,
                kind,
                context.single(CoreExtensions.commentsToContentConverter),
                context.logger,
                operation
            )
        }.pageForModule(module)

}