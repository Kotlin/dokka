package  org.jetbrains.dokka.kotlinAsJava


import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.transformers.documentation.DocumentablesToPageTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDescriptorToDocumentableTranslator by extending {
        CoreExtensions.descriptorToDocumentationTranslator providing ::KotlinAsJavaDescriptorToDocumentationTranslator
    }
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        CoreExtensions.documentablesToPageTranslator providing ::KotlinAsJavaDocumentationToPageTranslator
    }
}

object DescriptorCache {
    private val cache: HashMap<DRI, DeclarationDescriptor> = HashMap()

    fun add(dri: DRI, descriptor: DeclarationDescriptor): Boolean = cache.putIfAbsent(dri, descriptor) == null
    operator fun get(dri: DRI): DeclarationDescriptor? = cache[dri]
}

class KotlinAsJavaDocumentationToPageTranslator(
    private val context: DokkaContext
) : DocumentablesToPageTranslator {
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