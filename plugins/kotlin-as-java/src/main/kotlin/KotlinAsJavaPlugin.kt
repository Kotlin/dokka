package  org.jetbrains.dokka.kotlinAsJava


import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDescriptorToDocumentableTranslator by extending {
        CoreExtensions.descriptorToDocumentableTranslator providing ::KotlinAsJavaDescriptorToDocumentableTranslator
    }
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing ::KotlinAsJavaDocumentationToPageTranslator
    }
}

object DescriptorCache {
    private val cache: HashMap<DRI, DeclarationDescriptor> = HashMap()

    fun add(dri: DRI, descriptor: DeclarationDescriptor): Boolean = cache.putIfAbsent(dri, descriptor) == null
    operator fun get(dri: DRI): DeclarationDescriptor? = cache[dri]
}

class KotlinAsJavaDocumentationToPageTranslator(
    private val context: DokkaContext
) : DocumentableToPageTranslator {
    override fun invoke(module: Module): ModulePageNode =
        KotlinAsJavaPageBuilder { node, kind, operation ->
            KotlinAsJavaPageContentBuilder.group(
                setOf(node.dri),
                node.platformData,
                kind,
                context.plugin<DokkaBase>().querySingle { commentsToContentConverter },
                context.logger,
                operation
            )
        }.pageForModule(module)

}